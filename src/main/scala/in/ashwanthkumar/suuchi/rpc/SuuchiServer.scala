package in.ashwanthkumar.suuchi.rpc

import in.ashwanthkumar.suuchi.store.InMemoryStore
import io.grpc.{BindableService, ServerBuilder, Server}
import io.grpc.netty.NettyServerBuilder
import org.slf4j.LoggerFactory

class SuuchiServer(port: Int, services: List[BindableService] = Nil) {
  private val log = LoggerFactory.getLogger(classOf[SuuchiServer])

  private var server: Server = _
  private val serverBuilder: ServerBuilder[_] = NettyServerBuilder.forPort(port).addService(new PingService)

  def start() = {
    services.foreach(serverBuilder.addService)
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
  val server = new SuuchiServer(5051, List(new SuuchiReadService(store), new SuuchiPutService(store)))
  server.start()
  server.blockUntilShutdown()
}
