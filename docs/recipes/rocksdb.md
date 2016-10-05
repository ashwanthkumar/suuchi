# Distributed RocksDB backed KV

Following code builds a consistent hashing based Get/Put requests backed by an RocksDB.

```scala
package in.ashwanthkumar.suuchi.example

import java.nio.file.Files

import in.ashwanthkumar.suuchi.router.ConsistentHashingRouting
import in.ashwanthkumar.suuchi.rpc.Server._
import in.ashwanthkumar.suuchi.rpc.{Server, SuuchiPutService, SuuchiReadService}
import in.ashwanthkumar.suuchi.store.rocksdb.{RocksDbConfiguration, RocksDbStore}
import io.grpc.netty.NettyServerBuilder

object DistributedRocksDb extends App {
  val routingStrategy = ConsistentHashingRouting(2, whoami(5051), whoami(5052))

  val path1 = Files.createTempDirectory("distributed-rocksdb").toFile
  val store1 = new RocksDbStore(RocksDbConfiguration(path1.getAbsolutePath))
  val server1 = Server(NettyServerBuilder.forPort(5051), whoami(5051))
    .routeUsing(new SuuchiReadService(store1), routingStrategy)
    .withReplication(new SuuchiPutService(store1), 2, routingStrategy)
  server1.start()

  val path2 = Files.createTempDirectory("distributed-rocksdb").toFile
  val store2 = new RocksDbStore(RocksDbConfiguration(path2.getAbsolutePath))
  val server2 = Server(NettyServerBuilder.forPort(5052), whoami(5052))
    .routeUsing(new SuuchiReadService(store2), routingStrategy)
    .withReplication(new SuuchiPutService(store2), 2, routingStrategy)
  server2.start()

  server1.blockUntilShutdown()
  server2.blockUntilShutdown()

  /*
    Optionally if want to delete the rocksdb directory
      path1.delete()
      path2.delete()
  */
}
```

TODO - Explain the code.
