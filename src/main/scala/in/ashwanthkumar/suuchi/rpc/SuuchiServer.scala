package in.ashwanthkumar.suuchi.rpc

import in.ashwanthkumar.suuchi.store.InMemoryStore
import io.grpc.ServerCall.Listener
import io.grpc._
import io.grpc.netty.{NettyChannelBuilder, NettyServerBuilder}
import io.grpc.stub.ClientCalls
import org.slf4j.LoggerFactory

class SuuchiServer(port: Int, services: List[BindableService] = Nil, serviceSpecs: List[ServerServiceDefinition] = Nil) {
  private val log = LoggerFactory.getLogger(classOf[SuuchiServer])

  private var server: Server = _
  private val serverBuilder: ServerBuilder[_] = NettyServerBuilder.forPort(port).addService(new PingService)

  def start() = {
    services.foreach(serverBuilder.addService)
    serviceSpecs.foreach(serverBuilder.addService)
    server = serverBuilder
      .build()
      .start()
    log.info("Server started, listening on " + port)
    Runtime.getRuntime.addShutdownHook(new Thread() {
      override def run() {
        // Use stderr here since the logger may have been reset by its JVM shutdown hook.
        System.err.println("*** shutting down gRPC server since JVM is shutting down")
        SuuchiServer.this.stop()
        System.err.println("*** server shut down")
      }
    })
  }

  def stop() = {
    if (server != null) {
      server.shutdown()
    }
  }

  def blockUntilShutdown() = {
    if (server != null) {
      server.awaitTermination()
    }
  }
}

object SuuchiServer extends App {
  val store = new InMemoryStore
  val server1 = new SuuchiServer(5051, List(new SuuchiPutService(store)), List(ServerInterceptors.intercept(new SuuchiReadService(store), new RequestForwarder(5052))))
  server1.start()

  val server2 = new SuuchiServer(5052, List(new SuuchiReadService(store), new SuuchiPutService(store)))
  server2.start()

  server1.blockUntilShutdown()
  server2.blockUntilShutdown()
}

class RequestForwarder(port: Int) extends ServerInterceptor {
  private val log = LoggerFactory.getLogger(getClass)

  override def interceptCall[ReqT, RespT](serverCall: ServerCall[ReqT, RespT], headers: Metadata, next: ServerCallHandler[ReqT, RespT]): Listener[ReqT] = {
    log.debug("Intercepting " + serverCall.getMethodDescriptor.getFullMethodName + " method")
    new Listener[ReqT] {
      val delegate = next.startCall(serverCall, headers)

      override def onReady(): Unit = delegate.onReady()
      override def onMessage(incomingRequest: ReqT): Unit = {
        // TODO - Make the shouldForward pluggable
        val shouldForward = true
        log.info("Should forward? - " + shouldForward)
        if (shouldForward) {
          val forwarderChannel = NettyChannelBuilder.forAddress("localhost", port).usePlaintext(true).build()
          val clientResponse = ClientCalls.blockingUnaryCall(forwarderChannel, serverCall.getMethodDescriptor, CallOptions.DEFAULT, incomingRequest)
          forwarderChannel.shutdown()
          // sendHeaders is very important and should be called before sendMessage
          // else client wouldn't receive any data at all
          serverCall.sendHeaders(headers)
          serverCall.sendMessage(clientResponse)
        } else {
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
