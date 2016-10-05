# Distributed InMemory Database

Following code builds a consistent hashing based Get/Put requests backed by an ConcurrentHashMap.

```scala
package in.ashwanthkumar.suuchi.example

import in.ashwanthkumar.suuchi.membership.MemberAddress
import in.ashwanthkumar.suuchi.router.ConsistentHashingRouting
import in.ashwanthkumar.suuchi.rpc.Server.whoami
import in.ashwanthkumar.suuchi.rpc.{Server, SuuchiPutService, SuuchiReadService}
import in.ashwanthkumar.suuchi.store.InMemoryStore
import io.grpc.netty.NettyServerBuilder

object InMemoryDb extends App {
  val routingStrategy = ConsistentHashingRouting(whoami(5051), whoami(5052))

  val store1 = new InMemoryStore
  val server1 = Server(NettyServerBuilder.forPort(5051), whoami(5051))
    .routeUsing(new SuuchiReadService(store1), routingStrategy)
    .routeUsing(new SuuchiPutService(store1), routingStrategy)
  server1.start()

  val store2 = new InMemoryStore
  val server2 = Server(NettyServerBuilder.forPort(5052), whoami(5052))
    .routeUsing(new SuuchiReadService(store2), routingStrategy)
    .routeUsing(new SuuchiPutService(store2), routingStrategy)
  server2.start()

  server1.blockUntilShutdown()
  server2.blockUntilShutdown()
}
```

Let's break down the above code step by step.

1. `ConsistentHashingRouting` is a routing strategy that does routing between all the nodes using a ConsistentHashRing underneath with default vnode factor of 3.
2. `NettyServerBuilder.forPort(5051)` creates a NettyServer on `5051` port.
3. `server.routeUsing()` adds a new protob rpc using a routing strategy.
4. `server1.start()` starts the underlying gRPC server.
5. `server1.blockUntilShutdown()` waits until the server is stopped.
