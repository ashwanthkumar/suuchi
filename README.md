[![Build Status](https://snap-ci.com/ashwanthkumar/suuchi/branch/master/build_image)](https://snap-ci.com/ashwanthkumar/suuchi/branch/master)
[![codecov](https://codecov.io/gh/ashwanthkumar/suuchi/branch/master/graph/badge.svg)](https://codecov.io/gh/ashwanthkumar/suuchi)

# Suuchi - सूचि

Suuchi in sanskrit means an Index<sup>[1](http://spokensanskrit.de/index.php?tinput=sUci&direction=SE&script=HK&link=yes&beginning=0)</sup>.

## Getting Started

To get started in suuchi you need to add the following dependency in your pom.xml
```xml
<dependency>
    <groupId>in.ashwanthkumar</groupId>
    <artifactId>suuchi</artifactId>
    <version>${suuchi.version}</version>
</dependency>
```

Writing a distributed app using Suuchi is very easy. You can find example apps in `in.ashwanthkumar.suuchi.examples` package.

```scala
package in.ashwanthkumar.suuchi.example

import in.ashwanthkumar.suuchi.membership.MemberAddress
import in.ashwanthkumar.suuchi.router.ConsistentHashingRouting
import in.ashwanthkumar.suuchi.rpc.Server.whoami
import in.ashwanthkumar.suuchi.rpc.{Server, SuuchiPutService, SuuchiReadService}
import in.ashwanthkumar.suuchi.store.InMemoryStore
import io.grpc.netty.NettyServerBuilder

object ExampleApp extends App {
  val store = new InMemoryStore

  val routingStrategy = ConsistentHashingRouting(whoami(5051), whoami(5052))

  val server1 = Server(NettyServerBuilder.forPort(5051), whoami(5051))
    .routeUsing(new SuuchiReadService(store), routingStrategy)
    .routeUsing(new SuuchiPutService(store), routingStrategy)
  server1.start()

  val server2 = Server(NettyServerBuilder.forPort(5052), whoami(5052))
    .routeUsing(new SuuchiReadService(store), routingStrategy)
    .routeUsing(new SuuchiPutService(store), routingStrategy)
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

## Notes
If you're getting `ClassNotFound` exception, please run `mvn clean compile` once to generate the protoc files.
