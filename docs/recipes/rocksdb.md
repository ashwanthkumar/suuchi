# Distributed RocksDB backed KV

### Dependencies
```xml
<dependency>
    <groupId>in.ashwanthkumar</groupId>
    <artifactId>suuchi-core</artifactId>
    <version>${suuchi.version}</version>
</dependency>
<dependency>
    <groupId>in.ashwanthkumar</groupId>
    <artifactId>suuchi-rocksdb</artifactId>
    <version>${suuchi.version}</version>
</dependency>
```

### Code

Following code builds a consistent hashing based Get/Put requests backed by [RocksDB](https://github.com/facebook/rocksdb). It also does replication for Put requests to `REPLICATION_COUNT` number of nodes in the cluster.

```scala
package in.ashwanthkumar.suuchi.example

import java.nio.file.Files

import in.ashwanthkumar.suuchi.router.ConsistentHashingRouting
import in.ashwanthkumar.suuchi.rpc.Server._
import in.ashwanthkumar.suuchi.rpc.{Server, SuuchiPutService, SuuchiReadService}
import in.ashwanthkumar.suuchi.store.rocksdb.{RocksDbConfiguration, RocksDbStore}
import io.grpc.netty.NettyServerBuilder

object DistributedRocksDb extends App {
  val port = args(0).toInt

  val REPLICATION_COUNT = 2
  val PARTITIONS_PER_NODE = 50
  val routingStrategy = ConsistentHashingRouting(REPLICATION_COUNT, PARTITIONS_PER_NODE, whoami(5051), whoami(5052))

  val path = Files.createTempDirectory("distributed-rocksdb").toFile
  println(s"Using ${path.getAbsolutePath} for RocksDB")
  val store = new RocksDbStore(RocksDbConfiguration(path.getAbsolutePath))
  val server = Server(NettyServerBuilder.forPort(port), whoami(port))
    .routeUsing(new SuuchiReadService(store), routingStrategy)
    .withParallelReplication(new SuuchiPutService(store), REPLICATION_COUNT, routingStrategy)
  server.start()
  server.blockUntilShutdown()

}
```

This code is available as part of [`suuchi-examples`](https://github.com/ashwanthkumar/suuchi/tree/master/suuchi-examples) module in the repo.

To see this recipe in action, you might also want to look into the client which can talk to this service - [DistributedKVClient](kvclient.md).
