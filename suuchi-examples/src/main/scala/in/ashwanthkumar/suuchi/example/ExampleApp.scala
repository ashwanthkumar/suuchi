package in.ashwanthkumar.suuchi.example

import in.ashwanthkumar.suuchi.router.ConsistentHashingRouting
import in.ashwanthkumar.suuchi.rpc.Server.whoami
import in.ashwanthkumar.suuchi.rpc.{Server, SuuchiPutService, SuuchiReadService}
import in.ashwanthkumar.suuchi.store.InMemoryStore
import io.grpc.netty.NettyServerBuilder

object ExampleApp extends App {
  val routingStrategy = ConsistentHashingRouting(2, whoami(5051), whoami(5052), whoami(5053))

  val store1 = new InMemoryStore
  val server1 = Server(NettyServerBuilder.forPort(5051), whoami(5051))
    .routeUsing(new SuuchiReadService(store1), routingStrategy)
    .withSequentialReplication(new SuuchiPutService(store1), 2, routingStrategy)
  server1.start()

  val store2 = new InMemoryStore
  val server2 = Server(NettyServerBuilder.forPort(5052), whoami(5052))
    .routeUsing(new SuuchiReadService(store2), routingStrategy)
    .withSequentialReplication(new SuuchiPutService(store2), 2, routingStrategy)
  server2.start()

  val store3 = new InMemoryStore
  val server3 = Server(NettyServerBuilder.forPort(5053), whoami(5053))
    .routeUsing(new SuuchiReadService(store3), routingStrategy)
    .withSequentialReplication(new SuuchiPutService(store3), 2, routingStrategy)
  server3.start()

  server1.blockUntilShutdown()
  server2.blockUntilShutdown()
  server3.blockUntilShutdown()
}
