package in.ashwanthkumar.suuchi.example

import in.ashwanthkumar.suuchi.membership.MemberAddress
import in.ashwanthkumar.suuchi.router.ConsistentHashingRouting
import in.ashwanthkumar.suuchi.rpc.Server.whoami
import in.ashwanthkumar.suuchi.rpc.{Server, SuuchiPutService, SuuchiReadService}
import in.ashwanthkumar.suuchi.store.InMemoryStore
import io.grpc.netty.NettyServerBuilder

object ExampleApp extends App {
  val store = new InMemoryStore

  val routingStrategy = ConsistentHashingRouting(MemberAddress("localhost", 5051), MemberAddress("localhost", 5052))

  val server1 = Server(NettyServerBuilder.forPort(5051), whoami(5051))
    .routeUsing(new SuuchiReadService(store), routingStrategy)
    .routeUsing(new SuuchiPutService(store), routingStrategy)
  server1.start()

  val server2 = Server(NettyServerBuilder.forPort(5051), whoami(5051))
    .routeUsing(new SuuchiReadService(store), routingStrategy)
    .routeUsing(new SuuchiPutService(store), routingStrategy)
  server2.start()

  server1.blockUntilShutdown()
  server2.blockUntilShutdown()
}
