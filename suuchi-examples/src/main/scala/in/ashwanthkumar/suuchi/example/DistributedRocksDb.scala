package in.ashwanthkumar.suuchi.example

import java.nio.file.Files

import in.ashwanthkumar.suuchi.router.ConsistentHashingRouting
import in.ashwanthkumar.suuchi.rpc.Server._
import in.ashwanthkumar.suuchi.rpc.{Server, SuuchiPutService, SuuchiReadService}
import in.ashwanthkumar.suuchi.store.rocksdb.{RocksDbConfiguration, RocksDbStore}
import io.grpc.netty.NettyServerBuilder

object DistributedRocksDb extends App {
  val port = args(0).toInt

  val REPLICATION_COUNT   = 2
  val PARTITIONS_PER_NODE = 50
  val routingStrategy =
    ConsistentHashingRouting(REPLICATION_COUNT, PARTITIONS_PER_NODE, whoami(5051), whoami(5052))

  val path = Files.createTempDirectory("distributed-rocksdb").toFile
  println(s"Using ${path.getAbsolutePath} for RocksDB")
  val store = new RocksDbStore(RocksDbConfiguration(path.getAbsolutePath))
  val server = Server(NettyServerBuilder.forPort(port), whoami(port))
    .routeUsing(new SuuchiReadService(store), routingStrategy)
    .withParallelReplication(new SuuchiPutService(store), REPLICATION_COUNT, routingStrategy)
  server.start()
  server.blockUntilShutdown()

}
