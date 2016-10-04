package in.ashwanthkumar.suuchi.example

import in.ashwanthkumar.suuchi.router.ConsistentHashingRouting
import in.ashwanthkumar.suuchi.rpc.Server.whoami
import in.ashwanthkumar.suuchi.rpc.{Server, SuuchiPutService, SuuchiReadService}
import in.ashwanthkumar.suuchi.store.InMemoryStore
import io.grpc.netty.NettyServerBuilder

object ExampleApp extends App {
  val store = new InMemoryStore

  val routingStrategy = ConsistentHashingRouting(2, whoami(5051), whoami(5052), whoami(5053))

  val server1 = Server(NettyServerBuilder.forPort(5051), whoami(5051))
    .routeUsing(new SuuchiReadService(store), routingStrategy)
    .withReplication(new SuuchiPutService(store), 2, routingStrategy)
  server1.start()

  val server2 = Server(NettyServerBuilder.forPort(5052), whoami(5052))
    .routeUsing(new SuuchiReadService(store), routingStrategy)
    .withReplication(new SuuchiPutService(store), 2, routingStrategy)
  server2.start()

  val server3 = Server(NettyServerBuilder.forPort(5053), whoami(5053))
    .routeUsing(new SuuchiReadService(store), routingStrategy)
    .withReplication(new SuuchiPutService(store), 2, routingStrategy)
  server3.start()

  server1.blockUntilShutdown()
  server2.blockUntilShutdown()
  server3.blockUntilShutdown()
}
