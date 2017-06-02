package in.ashwanthkumar.suuchi.example

import in.ashwanthkumar.suuchi.router.ConsistentHashingRouting
import in.ashwanthkumar.suuchi.rpc.Server.whoami
import in.ashwanthkumar.suuchi.rpc.{Server, SuuchiPutService, SuuchiReadService, SuuchiScanService}
import in.ashwanthkumar.suuchi.store.InMemoryStore
import io.grpc.netty.NettyServerBuilder

// Start the app with either / one each of 5051, 5052 or/and 5053 port numbers
object DistributedKVServer extends App {

  val port                = args(0).toInt
  val PARTITIONS_PER_NODE = 100
  val REPLICATION_FACTOR  = 2

  val routingStrategy = ConsistentHashingRouting(REPLICATION_FACTOR,
                                                 PARTITIONS_PER_NODE,
                                                 whoami(5051),
                                                 whoami(5052),
                                                 whoami(5053))

  val store = new InMemoryStore
  val server = Server(NettyServerBuilder.forPort(port), whoami(port))
    .routeUsing(new SuuchiReadService(store), routingStrategy)
    .withParallelReplication(new SuuchiPutService(store), REPLICATION_FACTOR, routingStrategy)
    .withService(new SuuchiScanService(store))

  server.start()

  server.blockUntilShutdown()
}
