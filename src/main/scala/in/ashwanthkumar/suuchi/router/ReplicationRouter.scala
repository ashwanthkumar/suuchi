package in.ashwanthkumar.suuchi.router

import in.ashwanthkumar.suuchi.membership.MemberAddress
import io.grpc.Metadata.AsciiMarshaller
import io.grpc.ServerCall.Listener
import io.grpc._
import io.grpc.netty.NettyChannelBuilder
import io.grpc.stub.{ClientCalls, MetadataUtils}
import org.slf4j.LoggerFactory


/**
 * Replication Router picks up the set of nodes to which this request needs to be sent to (if not already set)
 * and forwards the request to the list of nodes in parallel and waits for all of them to complete
 */
abstract class ReplicationRouter(nrReplicas: Int, self: MemberAddress) extends ServerInterceptor { me =>
  import Headers._
  var forwarded = false

  protected val log = LoggerFactory.getLogger(me.getClass)

  override def interceptCall[ReqT, RespT](serverCall: ServerCall[ReqT, RespT], headers: Metadata, next: ServerCallHandler[ReqT, RespT]): Listener[ReqT] = {
    log.trace("Intercepting " + serverCall.getMethodDescriptor.getFullMethodName + " method in " + self)
    val replicator = this
    new Listener[ReqT] {
      val delegate = next.startCall(serverCall, headers)

      override def onReady(): Unit = delegate.onReady()
      override def onMessage(incomingRequest: ReqT): Unit = {
        log.trace("onMessage in replicator")
        if (headers.containsKey(REPLICATION_REQUEST_KEY) && headers.get(REPLICATION_REQUEST_KEY).equals(self.toString)) {
          log.info("Received replication request for {}, processing it", incomingRequest)
          delegate.onMessage(incomingRequest)
        } else if (headers.containsKey(ELIGIBLE_NODES_KEY)) {
          // since this isn't a replication request - replicate the request to list of nodes as defined in ELIGIBLE_NODES header
          val nodes = headers.get(ELIGIBLE_NODES_KEY)
          log.trace("Going to replicate the request to {}", nodes)
          replicator.replicate(nodes, serverCall, headers, incomingRequest, delegate)
          log.trace("Replication complete for {}", incomingRequest)
        } else {
          log.trace("Ignoring the request since I don't know what to do")
        }
      }

      override def onHalfClose(): Unit = {
        // apparently default ServerCall listener seems to hold some state from OnMessage which fails
        // here and client fails with an exception message -- Half-closed without a request
        if (replicator.forwarded) serverCall.close(Status.OK, headers) else delegate.onHalfClose()
      }
      override def onCancel(): Unit = delegate.onCancel()
      override def onComplete(): Unit = delegate.onComplete()
    }
  }

  def forward[RespT, ReqT](methodDescriptor: MethodDescriptor[ReqT, RespT], headers: Metadata, incomingRequest: ReqT, destination: MemberAddress): Any = {
    forwarded = true
    // Add HEADER to signify that this is a REPLICATION_REQUEST
    headers.put(REPLICATION_REQUEST_KEY, destination.toString)
    val nettyChannel = NettyChannelBuilder.forAddress(destination.host, destination.port).usePlaintext(true).build()

    val clientResponse = ClientCalls.blockingUnaryCall(
      ClientInterceptors.interceptForward(nettyChannel, MetadataUtils.newAttachHeadersInterceptor(headers)),
      methodDescriptor,
      CallOptions.DEFAULT,
      incomingRequest)

    nettyChannel.shutdown()
    clientResponse
  }

  /**
   * Subclasses can choose to implement on how they want to replicate.
   *
   * See [[SequentialReplicator]] for usage.
   */
  def replicate[ReqT, RespT](eligibleNodes: List[MemberAddress], serverCall: ServerCall[ReqT, RespT], headers: Metadata, incomingRequest: ReqT, delegate: ServerCall.Listener[ReqT]): Unit
}

class SequentialReplicator(nrReplicas: Int, self: MemberAddress) extends ReplicationRouter(nrReplicas, self) {
  override def replicate[ReqT, RespT](eligibleNodes: List[MemberAddress], serverCall: ServerCall[ReqT, RespT], headers: Metadata, incomingRequest: ReqT, delegate: Listener[ReqT]): Unit = {
    eligibleNodes match {
      case nodes if nodes.size < nrReplicas =>
        log.warn("We don't have enough nodes to satisfy the replication factor. Not processing this request")
        serverCall.close(Status.FAILED_PRECONDITION, headers)
      case nodes if nodes.nonEmpty =>
        log.info("Replication nodes for {} are {}", incomingRequest, nodes)
        log.debug("Sequentially sending out replication requests to the above set of nodes")
        var iamfound = false
        eligibleNodes.foreach {
          case node if node == self => iamfound = true
          case destination => forward(serverCall.getMethodDescriptor, headers, incomingRequest, destination)
        }
        // we need to push this after the forwarding else we return to client immediately saying we're done
        if (iamfound) {
          delegate.onMessage(incomingRequest)
          forwarded = false // FIXME - we had to unset this here since the actual service method is invoked only on onHalfClose()
        }
      case Nil =>
        log.error("This should never happen. No nodes found to place replica")
        serverCall.close(Status.INTERNAL, headers)
    }
  }
}