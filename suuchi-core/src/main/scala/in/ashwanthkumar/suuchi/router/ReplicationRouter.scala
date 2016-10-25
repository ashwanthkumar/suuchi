package in.ashwanthkumar.suuchi.router

import com.google.common.util.concurrent.{Futures, ListenableFuture}
import in.ashwanthkumar.suuchi.membership.MemberAddress
import in.ashwanthkumar.suuchi.rpc.CachedChannelPool
import in.ashwanthkumar.suuchi.utils.Logging
import io.grpc.ServerCall.Listener
import io.grpc._
import io.grpc.stub.{ClientCalls, MetadataUtils}

case class ReplicatorConfig(nrOfReplicas: Int,
                            self: MemberAddress,
                            headers: Metadata)

trait ReplicatorFactory {
  def create[ReqT](config: ReplicatorConfig, cachedChannelPool: CachedChannelPool): Replicator[ReqT]
}

/**
 * Replication Router picks up the set of nodes to which this request needs to be sent to (if not already set)
 * and forwards the request to the list of nodes in parallel and waits for all of them to complete
 */
abstract class ReplicationRouter(nrReplicas: Int, self: MemberAddress,
                                 factory: ReplicatorFactory) extends ServerInterceptor with Logging {me =>

  val channelPool = CachedChannelPool()

  override def interceptCall[ReqT, RespT](serverCall: ServerCall[ReqT, RespT], headers: Metadata, next: ServerCallHandler[ReqT, RespT]): Listener[ReqT] = {
    log.trace("Intercepting " + serverCall.getMethodDescriptor.getFullMethodName + " method in " + self)
    new Listener[ReqT] {
      var replicator: Replicator[ReqT] = factory.create[ReqT, RespT](ReplicatorConfig(nrReplicas, self, headers), channelPool)
      var forwarded = false
      val delegate = next.startCall(serverCall, headers)

      override def onReady(): Unit = {
        delegate.onReady()
        replicator.onReady()
      }
      override def onMessage(incomingRequest: ReqT): Unit = {
        log.trace("onMessage in replicator")
        if (headers.containsKey(Headers.REPLICATION_REQUEST_KEY) && headers.get(Headers.REPLICATION_REQUEST_KEY).equals(self.toString)) {
          log.debug("Received replication request for {}, processing it", incomingRequest)
          delegate.onMessage(incomingRequest)
        } else if (headers.containsKey(Headers.ELIGIBLE_NODES_KEY)) {
          // since this isn't a replication request - replicate the request to list of nodes as defined in ELIGIBLE_NODES header
          val nodes = headers.get(Headers.ELIGIBLE_NODES_KEY)
          // if the nodes to replicate contain self disable forwarded in all other cases forwarded is true
          // we need this since the default ServerHandler under which the actual delegate is wrapped under
          // invokes the method only in onHalfClose and not in onMessage (for non-streaming requests)
          forwarded = !nodes.contains(self)
          log.trace("Going to replicate the request to {}", nodes)
          replicator.replicate(nodes, incomingRequest, serverCall, delegate)
          log.trace("Replication complete for {}", incomingRequest)
        } else {
          log.trace("Ignoring the request since I don't know what to do")
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
}

/**
 * Subclasses can choose to implement on how they want to replicate.
 *
 * See [[SequentialReplicator]] for usage.
 */
abstract class Replicator[ReqT](config: ReplicatorConfig) extends Listener[ReqT] with Logging {

  final def replicate[RespT](eligibleNodes: List[MemberAddress],
                             incomingRequest: ReqT,
                             serverCall: ServerCall[ReqT, RespT],
                             delegate: ServerCall.Listener[ReqT]): Unit = {
    eligibleNodes match {
      case nodes if nodes.size < config.nrOfReplicas =>
        log.warn("We don't have enough nodes to satisfy the replication factor. Not processing this request")
        serverCall.close(Status.FAILED_PRECONDITION.withDescription("We don't have enough nodes to satisfy the replication factor. Not processing this request"), config.headers)
      case nodes if nodes.nonEmpty =>
        log.debug("Replication nodes for {} are {}", incomingRequest, nodes)
        doReplication(eligibleNodes, serverCall, config.headers, incomingRequest, delegate)
      case Nil =>
        log.error("This should never happen. No nodes found to place replica")
        serverCall.close(Status.INTERNAL.withDescription("This should never happen. No nodes found to place replica"), config.headers)
    }
  }

  /**
   * Implement the actual replication logic assuming that you've the right set of nodes.
   * Just do it!
   *
   * Error handling and other scenarios are handled at [[Replicator.replicate]]
   **/
  def doReplication[RespT](eligibleNodes: List[MemberAddress], serverCall: ServerCall[ReqT, RespT], headers: Metadata, incomingRequest: ReqT, delegate: ServerCall.Listener[ReqT]): Unit
}

/**
 * Sequential Synchronous replication implementation. While replicating we'll issue a forward request to each of the candidate nodes one by one.
 *
 * @param config Configuration for this Replicator
 * @param channelPool CachedChannelPool to use while replicating
 */
class SequentialReplicator[ReqT](config: ReplicatorConfig, channelPool: CachedChannelPool) extends Replicator[ReqT](config) {
  override def doReplication[RespT](eligibleNodes: List[MemberAddress], serverCall: ServerCall[ReqT, RespT], headers: Metadata, incomingRequest: ReqT, delegate: Listener[ReqT]) = {
    log.debug("Sequentially sending out replication requests to the above set of nodes")

    val hasLocalMember = eligibleNodes.exists(_.equals(config.self))

    eligibleNodes.filterNot(_.equals(config.self)).foreach { destination =>
      forward(serverCall.getMethodDescriptor, headers, incomingRequest, destination)
    }

    // we need to push this after the forwarding else we return to client immediately saying we're done
    if (hasLocalMember) {
      delegate.onMessage(incomingRequest)
    }
  }

  def forward[RespT](methodDescriptor: MethodDescriptor[ReqT, RespT], headers: Metadata, incomingRequest: ReqT, destination: MemberAddress): Any = {
    // Add HEADER to signify that this is a REPLICATION_REQUEST
    headers.put(Headers.REPLICATION_REQUEST_KEY, destination.toString)
    val channel = channelPool.get(destination, insecure = true)

    ClientCalls.blockingUnaryCall(
      ClientInterceptors.interceptForward(channel, MetadataUtils.newAttachHeadersInterceptor(headers)),
      methodDescriptor,
      CallOptions.DEFAULT,
      incomingRequest)
  }

}

/**
 * Parallel Synchronous replication implementation. While replicating we'll issue a forward request to all the nodes in
 * parallel. Even if one of the node's request fails the entire operation is assumed to have failed.
 *
 * @param config Configuration for this Replicator
 * @param channelPool CachedChannelPool to use while replicating
 */
class ParallelReplicator[ReqT](config: ReplicatorConfig, channelPool: CachedChannelPool) extends Replicator[ReqT](config) {
  override def doReplication[RespT](eligibleNodes: List[MemberAddress], serverCall: ServerCall[ReqT, RespT], headers: Metadata, incomingRequest: ReqT, delegate: Listener[ReqT]): Unit = {
    log.debug("Sending out replication requests to the above set of nodes in parallel")

    val hasLocalMember = eligibleNodes.exists(_.equals(config.self))

    val replicationResponses = eligibleNodes.filterNot(_.equals(config.self)).map { destination =>
      forwardAsync(serverCall.getMethodDescriptor, headers, incomingRequest, destination)
    }

    // Future.sequence equivalent + doing a get to ensure all operations complete
    log.debug("Waiting for replication response from replica nodes")
    Futures.allAsList(replicationResponses: _*).get()

    // we need to push this after the forwarding else we return to client immediately saying we're done
    if (hasLocalMember) {
      delegate.onMessage(incomingRequest)
    }
  }

  def forwardAsync[RespT](methodDescriptor: MethodDescriptor[ReqT, RespT], headers: Metadata,
                          incomingRequest: ReqT,
                          destination: MemberAddress): ListenableFuture[RespT] = {
    // Add HEADER to signify that this is a REPLICATION_REQUEST
    headers.put(Headers.REPLICATION_REQUEST_KEY, destination.toString)
    val channel = channelPool.get(destination, insecure = true)
    val clientCall = ClientInterceptors.interceptForward(channel, MetadataUtils.newAttachHeadersInterceptor(headers))
      .newCall(methodDescriptor, CallOptions.DEFAULT)
    ClientCalls.futureUnaryCall(clientCall, incomingRequest)
  }

}