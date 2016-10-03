package in.ashwanthkumar.suuchi.example

import java.io.File

import in.ashwanthkumar.suuchi.membership.MemberAddress
import in.ashwanthkumar.suuchi.router.ConsistentHashingRouting
import in.ashwanthkumar.suuchi.rpc.{SuuchiPutService, SuuchiReadService, Server}
import in.ashwanthkumar.suuchi.rpc.Server._
import in.ashwanthkumar.suuchi.store.InMemoryStore
import in.ashwanthkumar.suuchi.store.rocksdb.{RocksDbConfiguration, RocksDbStore}
import io.grpc.netty.NettyServerBuilder
import org.apache.commons.io.FileUtils

object DistributedRocksDb extends App {
  FileUtils.forceMkdir(new File("/tmp/distributed-rocksdb/1"))
  FileUtils.forceMkdir(new File("/tmp/distributed-rocksdb/2"))

  val store1 = new RocksDbStore(RocksDbConfiguration("/tmp/distributed-rocksdb/1"))
  val store2 = new RocksDbStore(RocksDbConfiguration("/tmp/distributed-rocksdb/2"))

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

  FileUtils.deleteQuietly(new File("/tmp/distributed-rocksdb"))

}
