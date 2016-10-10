package in.ashwanthkumar.suuchi.example

import in.ashwanthkumar.suuchi.router.ConsistentHashingRouting
import in.ashwanthkumar.suuchi.rpc.Server.whoami
import in.ashwanthkumar.suuchi.rpc.{Server, SuuchiPutService, SuuchiReadService}
import in.ashwanthkumar.suuchi.store.InMemoryStore
import io.grpc.netty.NettyServerBuilder

// Start the app with either / one each of 5051, 5052 or/and 5053 port numbers
object ExampleApp extends App {

  val port = args(0).toInt
  val replication = 2

  val routingStrategy = ConsistentHashingRouting(replication, whoami(5051), whoami(5052), whoami(5053))

  val store = new InMemoryStore
  val server = Server(NettyServerBuilder.forPort(port), whoami(port))
    .routeUsing(new SuuchiReadService(store), routingStrategy)
    .withParallelReplication(new SuuchiPutService(store), replication, routingStrategy)
  // .withSequentialReplication(new SuuchiPutService(store), replication, routingStrategy)
  // use the above when you want to replicate to one node at a time
  server.start()

  server.blockUntilShutdown()
}
