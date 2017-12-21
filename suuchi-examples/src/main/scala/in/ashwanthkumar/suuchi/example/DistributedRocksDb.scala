package in.ashwanthkumar.suuchi.example

import java.nio.file.Files
import java.util.concurrent.Executors

import in.ashwanthkumar.suuchi.example.DistributedKVServer.REPLICATION_FACTOR
import in.ashwanthkumar.suuchi.examples.rpc.generated.{PutGrpc, ReadGrpc}
import in.ashwanthkumar.suuchi.router.ConsistentHashingRouting
import in.ashwanthkumar.suuchi.rpc.Server._
import in.ashwanthkumar.suuchi.rpc.{Server, SuuchiPutService, SuuchiReadService}
import in.ashwanthkumar.suuchi.store.rocksdb.{RocksDbConfiguration, RocksDbStore}
import io.grpc.netty.NettyServerBuilder

import scala.concurrent.ExecutionContext

object DistributedRocksDb extends App {
  val port = args(0).toInt

  val REPLICATION_COUNT   = 2
  val PARTITIONS_PER_NODE = 50
  val routingStrategy =
    ConsistentHashingRouting(REPLICATION_COUNT, PARTITIONS_PER_NODE, whoami(5051), whoami(5052))

  val path = Files.createTempDirectory("distributed-rocksdb").toFile
  println(s"Using ${path.getAbsolutePath} for RocksDB")

  val cachedThreadPool = Executors.newCachedThreadPool()
  val executionContext = ExecutionContext.fromExecutor(cachedThreadPool)

  val store = new RocksDbStore(RocksDbConfiguration(path.getAbsolutePath))
  val server = Server(NettyServerBuilder.forPort(port), whoami(port))
    .routeUsing(ReadGrpc.bindService(new SuuchiReadService(store), executionContext), routingStrategy)
    .withParallelReplication(PutGrpc.bindService(new SuuchiPutService(store), executionContext), REPLICATION_FACTOR, routingStrategy)
  server.start()
  server.blockUntilShutdown()

}
