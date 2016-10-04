package in.ashwanthkumar.suuchi.router

import in.ashwanthkumar.suuchi.membership.MemberAddress
import in.ashwanthkumar.suuchi.rpc.SuuchiRPC.PutRequest
import io.grpc.Metadata.{AsciiMarshaller, AsciiKey}
import io.grpc.ServerCall.Listener
import io.grpc._
import io.grpc.netty.NettyChannelBuilder
import io.grpc.stub.{MetadataUtils, ClientCalls}
import org.slf4j.LoggerFactory


case object StringMarshaller extends AsciiMarshaller[String] {
  override def toAsciiString(value: String): String = value
  override def parseAsciiString(serialized: String): String = serialized
}
/**
 * Replication Router picks up the set of nodes to which this request needs to be sent to (if not already set)
 * and forwards the request to the list of nodes in parallel and waits for all of them to complete
 *
 * @param routingStrategy
 */
class ReplicationRouter(routingStrategy: RoutingStrategy, nrReplicas: Int, self: MemberAddress) extends ServerInterceptor {
  val REPLICATION_REQUEST = "replication_request"
  val REPLICATION_REQUEST_KEY = Metadata.Key.of(REPLICATION_REQUEST, StringMarshaller)
  private val log = LoggerFactory.getLogger(getClass)

  override def interceptCall[ReqT, RespT](serverCall: ServerCall[ReqT, RespT], headers: Metadata, next: ServerCallHandler[ReqT, RespT]): Listener[ReqT] = {
    log.trace("Intercepting " + serverCall.getMethodDescriptor.getFullMethodName + " method in " + self)
    new Listener[ReqT] {
      val delegate = next.startCall(serverCall, headers)
      var forwarded = false

      override def onReady(): Unit = delegate.onReady()
      override def onMessage(incomingRequest: ReqT): Unit = {
        if(headers.containsKey(REPLICATION_REQUEST_KEY) && headers.get(REPLICATION_REQUEST_KEY).equals(self.toString)) {
          log.info("Received replication request for {}, processing it", incomingRequest)
          delegate.onMessage(incomingRequest)
        }
        else {
          if (routingStrategy.route.isDefinedAt(incomingRequest)) {
            routingStrategy route incomingRequest match {
              case nodes if nodes.size < nrReplicas =>
                log.warn("We don't have enough nodes to satisfy the replication factor. Not processing this request")
                serverCall.close(Status.FAILED_PRECONDITION, headers)
              case nodes if nodes.nonEmpty =>
                log.info("Replication nodes for {} are {}", incomingRequest, nodes)
                log.debug("Sequentially sending out replication requests to the above set of nodes")
                nodes.foreach { node =>
                  forward(serverCall, headers, incomingRequest, node)
                }
              case _ =>
                log.error("This should never happen. No nodes found to place replica")
                serverCall.close(Status.INTERNAL, headers)
            }
          } else {
            log.trace("Calling delegate's onMessage since router can't understand this message")
            delegate.onMessage(incomingRequest)
          }
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

  def forward[RespT, ReqT](serverCall: ServerCall[ReqT, RespT], headers: Metadata, incomingRequest: ReqT, node: MemberAddress): RespT = {
    // Add HEADER to signify that this is a REPLICATION_REQUEST
    headers.put(Metadata.Key.of(REPLICATION_REQUEST, new AsciiMarshaller[String] {override def toAsciiString(value: String): String = value
      override def parseAsciiString(serialized: String): String = serialized
    }), node.toString)

    val nettyChannel = NettyChannelBuilder.forAddress(node.host, node.port).usePlaintext(true).build()

    val clientResponse = ClientCalls.blockingUnaryCall(
      ClientInterceptors.interceptForward(nettyChannel, MetadataUtils.newAttachHeadersInterceptor(headers)),
      serverCall.getMethodDescriptor,
      CallOptions.DEFAULT,
      incomingRequest)

    nettyChannel.shutdown()
    clientResponse
  }
}
