package in.ashwanthkumar.suuchi.rpc

import in.ashwanthkumar.suuchi.membership.MemberAddress
import in.ashwanthkumar.suuchi.router.{AlwaysRouteTo, ConsistentHashingRouter, Router}
import in.ashwanthkumar.suuchi.store.InMemoryStore
import io.grpc._
import io.grpc.netty.NettyServerBuilder
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
  val localRouter = new Router(new AlwaysRouteTo(MemberAddress("localhost", 5052)), MemberAddress("localhost", 5051))
  val chrRouter = new Router(ConsistentHashingRouter(), MemberAddress("localhost", 5052))

  val server1 = new SuuchiServer(5051,
    List(),
    List(
      ServerInterceptors.interceptForward(new SuuchiReadService(store), localRouter),
      ServerInterceptors.interceptForward(new SuuchiPutService(store), localRouter)
    )
  )
  server1.start()

  val server2 = new SuuchiServer(5052, List(new SuuchiReadService(store), new SuuchiPutService(store)))
  server2.start()

  server1.blockUntilShutdown()
  server2.blockUntilShutdown()
}
