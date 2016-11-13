package in.ashwanthkumar.suuchi.router

import java.util.concurrent.{Phaser, CountDownLatch, ConcurrentHashMap}
import in.ashwanthkumar.suuchi.membership.MemberAddress
import in.ashwanthkumar.suuchi.rpc.CachedChannelPool
import in.ashwanthkumar.suuchi.utils.Logging
import io.grpc.ServerCall.Listener
import io.grpc._
import io.grpc.stub.{ClientCallStreamObserver, StreamObserver}

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
class ReplicationRouter(nrReplicas: Int, self: MemberAddress,
                        factory: ReplicatorFactory) extends ServerInterceptor with Logging {me =>

  val channelPool = CachedChannelPool()

  override def interceptCall[ReqT, RespT](serverCall: ServerCall[ReqT, RespT], headers: Metadata, next: ServerCallHandler[ReqT, RespT]): Listener[ReqT] = {
    log.trace("Intercepting " + serverCall.getMethodDescriptor.getFullMethodName + " method in " + self)
    new Listener[ReqT] {
      var replicator: Replicator[ReqT] = factory.create[ReqT](ReplicatorConfig(nrReplicas, self, headers), channelPool)
      var forwarded = false
      val delegate = next.startCall(serverCall, headers)

      override def onReady(): Unit = {
        delegate.onReady()
        replicator.onReady()
      }
      override def onMessage(incomingRequest: ReqT): Unit = {
        log.trace("onMessage")
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
        serverCall.request(1)
      }

      override def onHalfClose(): Unit = {
        // apparently default ServerCall listener seems to hold some state from OnMessage which fails
        // here and client fails with an exception message -- Half-closed without a request
        replicator.onHalfClose()
        if (forwarded) serverCall.close(Status.OK, headers) else delegate.onHalfClose()
      }
      override def onCancel(): Unit = {
        replicator.onCancel()
        delegate.onCancel()
      }
      override def onComplete(): Unit = {
        replicator.onComplete()
        delegate.onComplete()
      }
    }
  }
}

/**
 * Subclasses can choose to implement on how they want to replicate.
 *
 * Life time of this object is lifetime of a streaming / normal request.
 *
 * See [[in.ashwanthkumar.suuchi.router.replication.ParallelReplicator]] for usage.
 */
abstract class Replicator[ReqT](config: ReplicatorConfig) extends Listener[ReqT] with Logging {

  import scala.collection.JavaConversions._

  protected val listenerMap = new ConcurrentHashMap[MemberAddress, StreamObserver[ReqT]]()

  protected var latch: CountDownLatch = new CountDownLatch(0)

  def replicate[RespT](eligibleNodes: List[MemberAddress],
                       incomingRequest: ReqT,
                       serverCall: ServerCall[ReqT, RespT],
                       delegate: ServerCall.Listener[ReqT]): Unit = {
    log.trace("Replicator#replicate")
    eligibleNodes match {
      case nodes if nodes.size < config.nrOfReplicas =>
        log.warn("We don't have enough nodes to satisfy the replication factor. Not processing this request")
        serverCall.close(Status.FAILED_PRECONDITION.withDescription("We don't have enough nodes to satisfy the replication factor. Not processing this request"), config.headers)
      case nodes if nodes.nonEmpty =>
        log.debug("Replication nodes for {} are {}", incomingRequest, nodes)
        latch = new CountDownLatch(nodes.size)
        doReplication(eligibleNodes, serverCall, config.headers, incomingRequest, delegate)
        log.trace("Replication is now complete for {}", nodes)
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

  override def onCancel(): Unit = {
    log.trace("onCancel")
    log.trace("Total listeners - " + listenerMap.size())
    listenerMap.values().foreach(_.onError(new Throwable("Request was cancelled")))
    log.trace("Waiting for onCancel -> latch.await" + s" ($latch)")
    latch.await()
    log.trace("Completed onCancel -> latch.await")
  }
  override def onHalfClose(): Unit = {
    log.trace("onHalfClose")
    log.trace("Total listeners - " + listenerMap.size())
    listenerMap.values().foreach(_.onCompleted())
    log.trace("Waiting for onHalfClose -> latch.await" + s" ($latch)")
    latch.await()
    log.trace("Completed onHalfClose -> latch.await")
  }
  override def onComplete(): Unit = {
    log.trace("onComplete")
  }
}
