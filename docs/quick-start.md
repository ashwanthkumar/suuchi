# Quick Start

Suuchi internally uses gRPC for it's communication. If you're new to gRPC, we recommend to follow the tutorial available on [grpc.io](http://www.grpc.io/docs/tutorials/basic/java.html). 

In the rest of this page we're going to assume you've the following setup

1. [Maven](index.md#maven) or [SBT](index.md#sbt) based project setup and suuchi dependencies configured already.
2. A ProtoBuf based service defined, and gRPC required classes generated.
3. You've also implemented at least one of Services defined in the proto.
4. Create a package `in.ashwanthkumar.suuchi.getting_started`
5. Create a class inside the above package as `DistributedKV.scala`, and copy-paste the code-snippet from the [Distributed Inmemory DB Recipe](recipes/inmemorydb.md).
6. Run the `DistribuedKV` object. Once it has started, run the `DistributedKVClient` object. This would make RPC calls to the server and get back a response and verify that it's valid.
7. That's it! - you've now built a distributed, partitioned and replicated memory backed KVStore.
