package in.ashwanthkumar.suuchi.router

import java.util.concurrent.{ConcurrentHashMap, Phaser}
import in.ashwanthkumar.suuchi.membership.MemberAddress
import in.ashwanthkumar.suuchi.rpc.CachedChannelPool
import in.ashwanthkumar.suuchi.utils.Logging
import io.grpc.MethodDescriptor.MethodType
import io.grpc.ServerCall.Listener
import io.grpc._
import io.grpc.stub.{ClientCallStreamObserver, ClientCalls, MetadataUtils, StreamObserver}
import org.slf4j.LoggerFactory

import scala.language.postfixOps
import scala.util.Try

/**
 * Router decides to route the incoming request to right node in the cluster as defined
 * by the [[RoutingStrategy]].
 *
 * @param routingStrategy
 */
class HandleOrForwardRouter(routingStrategy: RoutingStrategy, self: MemberAddress) extends ServerInterceptor {
  private val log = LoggerFactory.getLogger(getClass)
  val channelPool = CachedChannelPool()

  override def interceptCall[ReqT, RespT](serverCall: ServerCall[ReqT, RespT], headers: Metadata, next: ServerCallHandler[ReqT, RespT]): Listener[ReqT] = {
    log.trace("Intercepting " + serverCall.getMethodDescriptor.getFullMethodName + " method in " + self + " type=" + serverCall.getMethodDescriptor.getType + ", headers= " + headers.toString)
    new Listener[ReqT] {
      val delegate = next.startCall(serverCall, headers)
      var forwarded = false
      lazy val forwarder = serverCall.getMethodDescriptor.getType match {
        case MethodType.UNARY => new ForwardUnaryMethod[ReqT, RespT](serverCall, headers, channelPool)
        case MethodType.CLIENT_STREAMING => new ForwardClientStreamingMethod[ReqT, RespT](serverCall, headers, channelPool)
        // TODO - Need to support for other types of requests
        case idontknow => throw new RuntimeException(s"We don't know to handle / forward $idontknow method type")
      }

      override def onReady(): Unit = {
        delegate.onReady()
        forwarder.onReady()
      }
      override def onMessage(incomingRequest: ReqT): Unit = {
        log.trace("onMessage")
        log.trace("Incoming Request - " + incomingRequest)
        if (routingStrategy.route.isDefinedAt(incomingRequest)) {
          val eligibleNodes = routingStrategy route incomingRequest
          // Always set ELIGIBLE_NODES header to the list of nodes eligible in the current
          // operation - as defined by the RoutingStrategy
          headers.put(Headers.ELIGIBLE_NODES_KEY, eligibleNodes)

          eligibleNodes match {
            case nodes if nodes.nonEmpty && !nodes.exists(_.equals(self)) =>
              log.trace("Forwarding request to any of " + nodes.mkString(","))
              forwarded = forwarder.onMessage(incomingRequest, nodes)

              if (!forwarded) {
                serverCall.close(Status.FAILED_PRECONDITION.withDescription("No alive nodes to handle traffic."), headers)
              }
            case nodes if nodes.nonEmpty && nodes.exists(_.equals(self)) =>
              log.trace("Calling delegate's onMessage")
              delegate.onMessage(incomingRequest)
            case Nil =>
              log.trace("Couldn't locate the right node for this request. Returning a NOT_FOUND response")
              serverCall.close(Status.NOT_FOUND, headers)
          }
        } else {
          log.trace("Calling delegate's onMessage since router can't understand this message")
          delegate.onMessage(incomingRequest)
        }
        serverCall.request(1)
      }

      override def onHalfClose(): Unit = {
        log.trace("onHalfClose")
        forwarder.onHalfClose()
        // apparently default ServerCall listener seems to hold some state from OnMessage which fails
        // here and client fails with an exception message -- Half-closed without a request
        if (forwarded) serverCall.close(Status.OK, headers) else delegate.onHalfClose()
      }
      override def onCancel(): Unit = {
        log.trace("onCancel")
        forwarder.onCancel()
        delegate.onCancel()
      }
      override def onComplete(): Unit = {
        log.trace("onComplete")
        forwarder.onComplete()
        delegate.onComplete()
      }
    }
  }
}

trait Forwarder[ReqT] extends Logging {
  /**
   * Forward the message for an Unary Method
   *
   * @param message Message to forward
   * @param nodes Set of nodes where we can forward the request for an answer
   * @return  true if we were able to forward the message
   *          false otherwise
   */
  def onMessage(message: ReqT, nodes: List[MemberAddress]): Boolean
  def onCancel(): Unit = log.trace("onCancel")
  def onComplete(): Unit = log.trace("onComplete")
  def onReady(): Unit = log.trace("onReady")
  def onHalfClose(): Unit = log.trace("onHalfClose")

}

class ForwardUnaryMethod[ReqT, RespT](serverCall: ServerCall[ReqT, RespT], headers: Metadata, channelPool: CachedChannelPool) extends Forwarder[ReqT] with Logging {
  /**
   * Forward the message for an Unary Method
   *
   * @param message Message to forward
   * @param nodes Set of nodes where we can forward the request for an answer
   * @return  true if we were able to forward the message
   *          false otherwise
   */
  def onMessage(message: ReqT, nodes: List[MemberAddress]): Boolean = {
    nodes.exists(destination =>
      Try {
        log.trace(s"Forwarding request to $destination")
        val clientResponse: RespT = forward(serverCall.getMethodDescriptor, headers, message, destination)
        // sendHeaders is very important and should be called before sendMessage
        // else client wouldn't receive any data at all
        serverCall.sendHeaders(headers)
        serverCall.sendMessage(clientResponse)
        true
      } recover {
        case r: RuntimeException =>
          log.error(r.getMessage, r)
          false
      } get
    )
  }

  private def forward(method: MethodDescriptor[ReqT, RespT], headers: Metadata, incomingRequest: ReqT, destination: MemberAddress): RespT = {
    val channel = channelPool.get(destination, insecure = true)
    ClientCalls.blockingUnaryCall(
      ClientInterceptors.interceptForward(channel, MetadataUtils.newAttachHeadersInterceptor(headers)),
      method,
      CallOptions.DEFAULT,
      incomingRequest)
  }
}

class ForwardClientStreamingMethod[ReqT, RespT](serverCall: ServerCall[ReqT, RespT], headers: Metadata, channelPool: CachedChannelPool) extends Forwarder[ReqT] with Logging {

  import scala.collection.JavaConversions._

  val listenerMap = new ConcurrentHashMap[MemberAddress, StreamObserver[ReqT]]()
  val phaser = new Phaser()

  /**
   * Forward the message for a Streaming Method
   *
   * @param message Message to forward
   * @param nodes Set of nodes where we can forward the request for an answer
   * @return  true if we were able to forward the message
   *          false otherwise
   */
  override def onMessage(message: ReqT, nodes: List[MemberAddress]): Boolean = {
    log.trace("onMessage")
    nodes.exists(destination =>
      Try {
        log.trace(s"Forwarding request to $destination")
        forward(serverCall.getMethodDescriptor, headers, message, destination)
        log.trace(s"Forwarding request to $destination - Done")
        true
      } recover {
        case r: RuntimeException =>
          log.error(r.getMessage, r)
          false
      } get
    )
  }

  override def onCancel(): Unit = {
    super.onCancel()
    log.trace("onCancel")
    if (phaser.getRegisteredParties > 0) {
      listenerMap.values().foreach(_.onError(new Throwable("Request was cancelled")))
      log.trace(s"Waiting on onCancel -> phaser.awaitAdvance ($phaser)")
      phaser.awaitAdvance(phaser.getPhase)
      log.trace(s"Completed onCancel -> phaser.awaitAdvance ($phaser)")
    }
  }
  override def onComplete(): Unit = {
    super.onComplete()
    log.trace("onComplete")
  }
  override def onHalfClose(): Unit = {
    super.onHalfClose()
    log.trace("onHalfClose")
    listenerMap.values().foreach(_.onCompleted())
    if (phaser.getRegisteredParties > 0) {
      log.trace(s"Waiting on onHalfClose -> phaser.awaitAdvance ($phaser)")
      phaser.awaitAdvance(phaser.getPhase)
      log.trace(s"Completed onHalfClose -> phaser.awaitAdvance ($phaser)")
    }
  }

  private def forward(method: MethodDescriptor[ReqT, RespT], headers: Metadata, message: ReqT, destination: MemberAddress): Unit = {
    val observer: ClientCallStreamObserver[ReqT] = getOrCreateObserver(method, headers, destination).asInstanceOf[ClientCallStreamObserver[ReqT]]

    // TODO(ashwanthkumar) - Extract this exponential back-off as re-usable function
    // PS: also used in ParallelReplicator
    var count = 0
    while (!observer.isReady) {
      // TODO: Set upper limit on his value
      val sleepDuration: Long = math.pow(2.0, count).toInt * 100
      log.debug("Waiting for " + sleepDuration + "ms forward channel to become ready to node - " + destination)
      Thread.sleep(sleepDuration)
      count += 1
    }
    observer.onNext(message)
  }

  private def getOrCreateObserver(method: MethodDescriptor[ReqT, RespT], headers: Metadata, destination: MemberAddress): StreamObserver[ReqT] = {
    log.trace("getOrCreateObserver")
    if (listenerMap.containsKey(destination)) {
      log.trace(destination + " is already cached")
      listenerMap.get(destination)
    } else {
      log.trace(destination + " is NOT cached. Opening a new connection")
      val channel = channelPool.get(destination, insecure = true)
      val clientCall = ClientInterceptors.interceptForward(channel, MetadataUtils.newAttachHeadersInterceptor(headers))
        .newCall(method, CallOptions.DEFAULT)
      val observer = ClientCalls.asyncClientStreamingCall(
        clientCall, new ForwardStreamingResponseObserver[ReqT, RespT](phaser, serverCall, headers))
      log.trace(destination + " is being cached")
      listenerMap.put(destination, observer)
      log.trace(destination + " is cached. Returning the cached response")
      observer
    }
  }
}

class ForwardStreamingResponseObserver[ReqT, RespT](phaser: Phaser,
                                                    serverCall: ServerCall[ReqT, RespT],
                                                    headers: Metadata) extends StreamObserver[RespT] with Logging {

  phaser.register()
  var headersSent = false

  override def onError(t: Throwable): Unit = {
    log.trace("ForwardStreamingResponseObserver#onError")
    phaser.arrive()
    log.error(t.getMessage, t)
    throw t
  }

  override def onCompleted(): Unit = {
    log.trace("ForwardStreamingResponseObserver#onCompleted")
    phaser.arrive()
  }

  override def onNext(value: RespT): Unit = {
    log.trace("ForwardStreamingResponseObserver#onNext")
    // Send headers only if it's not been sent so far
    if (!headersSent) {
      serverCall.sendHeaders(headers)
      headersSent = true
    }
    serverCall.sendMessage(value)
  }

}