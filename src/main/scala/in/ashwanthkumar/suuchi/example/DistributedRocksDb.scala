package in.ashwanthkumar.suuchi.example

import java.io.File
import java.nio.file.Files

import in.ashwanthkumar.suuchi.membership.MemberAddress
import in.ashwanthkumar.suuchi.router.ConsistentHashingRouting
import in.ashwanthkumar.suuchi.rpc.{SuuchiPutService, SuuchiReadService, Server}
import in.ashwanthkumar.suuchi.rpc.Server._
import in.ashwanthkumar.suuchi.store.InMemoryStore
import in.ashwanthkumar.suuchi.store.rocksdb.{RocksDbConfiguration, RocksDbStore}
import io.grpc.netty.NettyServerBuilder
import org.apache.commons.io.FileUtils

object DistributedRocksDb extends App {
  val path1 = Files.createTempDirectory("distributed-rocksdb").toFile
  val path2 = Files.createTempDirectory("distributed-rocksdb").toFile

  val store1 = new RocksDbStore(RocksDbConfiguration(path1.getAbsolutePath))
  val store2 = new RocksDbStore(RocksDbConfiguration(path2.getAbsolutePath))

  val routingStrategy = ConsistentHashingRouting(whoami(5051), whoami(5052))

  val server1 = Server(NettyServerBuilder.forPort(5051), whoami(5051))
    .routeUsing(new SuuchiReadService(store1), routingStrategy)
    .routeUsing(new SuuchiPutService(store1), routingStrategy)
  server1.start()

  val server2 = Server(NettyServerBuilder.forPort(5052), whoami(5052))
    .routeUsing(new SuuchiReadService(store2), routingStrategy)
    .routeUsing(new SuuchiPutService(store2), routingStrategy)
  server2.start()

  server1.blockUntilShutdown()
  server2.blockUntilShutdown()

  path1.delete()
  path2.delete()

}
