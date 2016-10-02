package in.ashwanthkumar.suuchi.router

import in.ashwanthkumar.suuchi.membership.MemberAddress
import io.grpc.ServerCall.Listener
import io.grpc._
import io.grpc.netty.NettyChannelBuilder
import io.grpc.stub.ClientCalls
import org.slf4j.LoggerFactory

/**
 * Router decides to route the incoming request to right node in the cluster as defined
 * by the [[RoutingStrategy]].
 *
 * @param routingStrategy
 */
class Router(routingStrategy: RoutingStrategy, self: MemberAddress) extends ServerInterceptor {
  private val log = LoggerFactory.getLogger(getClass)

  override def interceptCall[ReqT, RespT](serverCall: ServerCall[ReqT, RespT], headers: Metadata, next: ServerCallHandler[ReqT, RespT]): Listener[ReqT] = {
    log.debug("Intercepting " + serverCall.getMethodDescriptor.getFullMethodName + " method")
    new Listener[ReqT] {
      val delegate = next.startCall(serverCall, headers)
      var forwarded = false

      override def onReady(): Unit = delegate.onReady()
      override def onMessage(incomingRequest: ReqT): Unit = {
        // TODO - Handle forwarding loop here
        if(routingStrategy.route.isDefinedAt(incomingRequest)) {
          routingStrategy route incomingRequest match {
            case Some(node) if !node.equals(self) =>
              log.debug(s"Forwarding request to $node")
              val forwarderChannel = NettyChannelBuilder.forAddress(node.host, node.port).usePlaintext(true).build()
              val clientResponse = ClientCalls.blockingUnaryCall(forwarderChannel, serverCall.getMethodDescriptor, CallOptions.DEFAULT, incomingRequest)
              forwarderChannel.shutdown()
              // sendHeaders is very important and should be called before sendMessage
              // else client wouldn't receive any data at all
              serverCall.sendHeaders(headers)
              serverCall.sendMessage(clientResponse)
              forwarded = true
            case _ =>
              log.debug("Calling delegate's onMessage")
              delegate.onMessage(incomingRequest)
          }
        } else {
          log.debug("Calling delegate's onMessage since router can't understand this message")
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
}
