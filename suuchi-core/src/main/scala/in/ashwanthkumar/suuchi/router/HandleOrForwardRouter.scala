package in.ashwanthkumar.suuchi.router

import in.ashwanthkumar.suuchi.cluster.MemberAddress
import in.ashwanthkumar.suuchi.rpc.CachedChannelPool
import io.grpc.ServerCall.Listener
import io.grpc._
import io.grpc.stub.{ClientCalls, MetadataUtils}
import org.slf4j.LoggerFactory

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
    log.trace("Intercepting " + serverCall.getMethodDescriptor.getFullMethodName + " method in " + self + ", headers= " + headers.toString)
    new Listener[ReqT] {
      val delegate = next.startCall(serverCall, headers)
      var forwarded = false

      override def onReady(): Unit = delegate.onReady()
      override def onMessage(incomingRequest: ReqT): Unit = {
        if (routingStrategy.route.isDefinedAt(incomingRequest)) {
          val eligibleNodes = routingStrategy route incomingRequest
          // Always set ELIGIBLE_NODES header to the list of nodes eligible in the current
          // operation - as defined by the RoutingStrategy
          headers.put(Headers.ELIGIBLE_NODES_KEY, eligibleNodes)

          eligibleNodes match {
            case nodes if nodes.nonEmpty && !nodes.exists(_.equals(self)) =>
              val destination = nodes.head
              log.trace(s"Forwarding request to $destination")
              val clientResponse: RespT = forward(serverCall.getMethodDescriptor, headers, incomingRequest, destination)
              // sendHeaders is very important and should be called before sendMessage
              // else client wouldn't receive any data at all
              serverCall.sendHeaders(headers)
              serverCall.sendMessage(clientResponse)
              forwarded = true
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
      }

      override def onHalfClose(): Unit = {
        // apparently default ServerCall listener seems to hold some state from OnMessage which fails
        // here and client fails with an exception message -- Half-closed without a request
        if (forwarded) serverCall.close(Status.OK, headers) else delegate.onHalfClose()
      }
      override def onCancel(): Unit = delegate.onCancel()
      override def onComplete(): Unit = delegate.onComplete()
    }
  }

  def forward[RespT, ReqT](method: MethodDescriptor[ReqT, RespT], headers: Metadata, incomingRequest: ReqT, destination: MemberAddress): RespT = {
    val channel = channelPool.get(destination, insecure = true)
    ClientCalls.blockingUnaryCall(
      ClientInterceptors.interceptForward(channel, MetadataUtils.newAttachHeadersInterceptor(headers)),
      method,
      CallOptions.DEFAULT,
      incomingRequest)
  }
}
