# Distributed InMemory Database

Following code builds a consistent hashing based Get/Put requests backed by an ConcurrentHashMap in memory.

```scala
package in.ashwanthkumar.suuchi.example

import java.nio.ByteBuffer

import in.ashwanthkumar.suuchi.client.SuuchiClient
import in.ashwanthkumar.suuchi.router.ConsistentHashingRouting
import in.ashwanthkumar.suuchi.rpc.Server.whoami
import in.ashwanthkumar.suuchi.rpc.{Server, SuuchiPutService, SuuchiReadService}
import in.ashwanthkumar.suuchi.store.InMemoryStore
import io.grpc.netty.NettyServerBuilder

object DistributedKVServer extends App {
  val port = args(0).toInt
  val PARTITIONS_PER_NODE = 100
  val REPLICATION_FACTOR = 2

  val routingStrategy = ConsistentHashingRouting(REPLICATION_FACTOR, PARTITIONS_PER_NODE, whoami(5051), whoami(5052), whoami(5053))

  val store = new InMemoryStore
  val server = Server(NettyServerBuilder.forPort(port), whoami(port))
    .routeUsing(new SuuchiReadService(store), routingStrategy)
    .withParallelReplication(new SuuchiPutService(store), REPLICATION_FACTOR, routingStrategy)
  server.start()

  server.blockUntilShutdown()
}
```

Let's break down the above code step by step.

- `ConsistentHashingRouting` is a [_Routing Strategy_](../internals/router.md#routingstrategy) that does routing between all the nodes using a ConsistentHashRing underneath with default vnode factor of 3.
- `NettyServerBuilder.forPort(5051)` creates a NettyServer on `5051` port.
- `server.routeUsing()` adds a new protobuf rpc using a custom routing strategy behind [_HandleOrForward_](../internals/router.md) router.
- `server.withParallelReplication()` adds a new protobuf rpc using the ReplicationRouter. By default it wraps both [_HandleOrForward_](../internals/router.md) and [_Replicator_](../internals/replication.md) routers.
- `server1.start()` starts the underlying gRPC server.
- `server1.blockUntilShutdown()` waits until the server is stopped.

To see this recipe in action, you might also want to look into the client which can talk to this service - [DistributedKVClient](kvclient.md).
