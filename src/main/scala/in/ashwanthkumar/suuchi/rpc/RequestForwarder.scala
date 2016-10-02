package in.ashwanthkumar.suuchi.rpc

import io.grpc.ServerCall.Listener
import io.grpc._
import io.grpc.netty.NettyChannelBuilder
import io.grpc.stub.ClientCalls
import org.slf4j.LoggerFactory

/**
 * RequestForwarder decides to forward the incoming request to right node in the cluster as defined
 * by the [[ForwardStrategy]].
 * @param forwardStrategy
 */
class RequestForwarder(forwardStrategy: ForwardStrategy) extends ServerInterceptor {
  private val log = LoggerFactory.getLogger(getClass)

  override def interceptCall[ReqT, RespT](serverCall: ServerCall[ReqT, RespT], headers: Metadata, next: ServerCallHandler[ReqT, RespT]): Listener[ReqT] = {
    log.debug("Intercepting " + serverCall.getMethodDescriptor.getFullMethodName + " method")
    new Listener[ReqT] {
      val delegate = next.startCall(serverCall, headers)

      override def onReady(): Unit = delegate.onReady()
      override def onMessage(incomingRequest: ReqT): Unit = {
        // TODO - Handle forwarding loop here
        forwardStrategy shouldForward incomingRequest match {
          case Some(node) =>
            log.debug(s"Forwarding request to $node")
            val forwarderChannel = NettyChannelBuilder.forAddress(node.host, node.port).usePlaintext(true).build()
            val clientResponse = ClientCalls.blockingUnaryCall(forwarderChannel, serverCall.getMethodDescriptor, CallOptions.DEFAULT, incomingRequest)
            forwarderChannel.shutdown()
            // sendHeaders is very important and should be called before sendMessage
            // else client wouldn't receive any data at all
            serverCall.sendHeaders(headers)
            serverCall.sendMessage(clientResponse)
          case None =>
            log.debug("Calling delegate's onMessage")
            // handle the call internally
            delegate.onMessage(incomingRequest)
        }
      }

      override def onHalfClose(): Unit = delegate.onHalfClose()
      override def onCancel(): Unit = delegate.onCancel()
      override def onComplete(): Unit = delegate.onComplete()
    }
  }
}
