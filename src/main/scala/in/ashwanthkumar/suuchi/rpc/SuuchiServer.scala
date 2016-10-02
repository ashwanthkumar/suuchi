package in.ashwanthkumar.suuchi.rpc

import in.ashwanthkumar.suuchi.membership.MemberAddress
import in.ashwanthkumar.suuchi.partitioner.{SuuchiHash, ConsistentHashRing, ConsistentHashPartitioner}
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
  val ring = new ConsistentHashRing(SuuchiHash).add(MemberAddress("localhost", 5051)).add(MemberAddress("localhost", 5052))

  val server1Router = new Router(new ConsistentHashingRouter(new ConsistentHashPartitioner(ring)), MemberAddress("localhost", 5051))
  val server2Router = new Router(new ConsistentHashingRouter(new ConsistentHashPartitioner(ring)), MemberAddress("localhost", 5052))

  val server1 = new SuuchiServer(5051,
    List(),
    List(
      ServerInterceptors.interceptForward(new SuuchiReadService(store), server1Router),
      ServerInterceptors.interceptForward(new SuuchiPutService(store), server1Router)
    )
  )
  server1.start()

  val server2 = new SuuchiServer(5052,
    List(),
    List(
      ServerInterceptors.interceptForward(new SuuchiReadService(store), server2Router),
      ServerInterceptors.interceptForward(new SuuchiPutService(store), server2Router)
    )
  )
  server2.start()

  server1.blockUntilShutdown()
  server2.blockUntilShutdown()
}
