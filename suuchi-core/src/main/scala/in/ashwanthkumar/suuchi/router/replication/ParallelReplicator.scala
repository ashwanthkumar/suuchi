package in.ashwanthkumar.suuchi.router.replication

import com.google.common.util.concurrent.{Futures, ListenableFuture}
import in.ashwanthkumar.suuchi.membership.MemberAddress
import in.ashwanthkumar.suuchi.router.{Headers, Replicator, ReplicatorConfig, ReplicatorFactory}
import in.ashwanthkumar.suuchi.rpc.CachedChannelPool
import io.grpc.ServerCall.Listener
import io.grpc._
import io.grpc.stub.{ClientCallStreamObserver, StreamObserver, ClientCalls, MetadataUtils}

/**
 * Parallel Synchronous replication implementation. While replicating we'll issue a forward request to all the nodes in
 * parallel. Even if one of the node's request fails the entire operation is assumed to have failed.
 *
 * @param config Configuration for this Replicator
 * @param channelPool CachedChannelPool to use while replicating
 */
class ParallelReplicator[ReqT](config: ReplicatorConfig, channelPool: CachedChannelPool) extends Replicator[ReqT](config) {
  override def doReplication[RespT](eligibleNodes: List[MemberAddress],
                                    serverCall: ServerCall[ReqT, RespT],
                                    headers: Metadata,
                                    incomingRequest: ReqT,
                                    delegate: Listener[ReqT]): Unit = {
    log.debug("Sending out replication requests to the above set of nodes in parallel")

    val hasLocalMember = eligibleNodes.exists(_.equals(config.self))

    eligibleNodes.filterNot(_.equals(config.self)).foreach { destination =>
      forwardAsync(serverCall.getMethodDescriptor, headers, incomingRequest, destination)
    }

    if (hasLocalMember) {
      log.trace("delegate#onMessage")
      delegate.onMessage(incomingRequest)
      latch.countDown()
    }
    log.trace(serverCall.attributes().keys().toString)
  }

  def forwardAsync[RespT](method: MethodDescriptor[ReqT, RespT], headers: Metadata,
                          message: ReqT,
                          destination: MemberAddress): Unit = {
    // Add HEADER to signify that this is a REPLICATION_REQUEST
    headers.put(Headers.REPLICATION_REQUEST_KEY, destination.toString)
    log.info(s"forwardAsync to $destination")

    val observer: ClientCallStreamObserver[ReqT] = getOrCreateStream(method, headers, destination).asInstanceOf[ClientCallStreamObserver[ReqT]]

    // TODO(ashwanthkumar) - Extract this exponential back-off as re-usable function
    // PS: also used in HandleOrForwardRouter
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

  def getOrCreateStream[RespT](methodDescriptor: MethodDescriptor[ReqT, RespT], headers: Metadata, destination: MemberAddress) = {
    if (listenerMap.containsKey(destination)) {
      listenerMap.get(destination)
    } else {
      val channel = channelPool.get(destination, insecure = true)
      val clientCall = ClientInterceptors.interceptForward(channel, MetadataUtils.newAttachHeadersInterceptor(headers))
        .newCall(methodDescriptor, CallOptions.DEFAULT)

      val observer = ClientCalls.asyncClientStreamingCall(clientCall, new StreamObserver[RespT] {
        override def onError(t: Throwable): Unit = {
          latch.countDown()
          log.error(t.getMessage, t)
          throw t
        }
        override def onCompleted(): Unit = {
          log.trace("getOrCreateStreamObserver#onCompleted")
          latch.countDown()
        }
        override def onNext(value: RespT): Unit = {
          log.trace("getOrCreateStreamObserver#onNext")
          log.debug("Response got from server - {}", value)
        }
      })

      listenerMap.put(destination, observer)
      observer
    }
  }
}

object ParallelReplicatorFactory extends ReplicatorFactory {
  override def create[ReqT](config: ReplicatorConfig, cachedChannelPool: CachedChannelPool): Replicator[ReqT] = {
    new ParallelReplicator[ReqT](config, cachedChannelPool)
  }
}