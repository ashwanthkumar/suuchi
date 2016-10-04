# Suuchi Release 0.1
What you get?
- [ ] Ability for nodes to join a cluster - de-scoped - Tracked as part of [#20](https://github.com/ashwanthkumar/suuchi/issues/20)
- [x] Ability to route traffic to different nodes based on CH strategy
- [x] Ability to write data and read data from the cluster
- [x] Pluggable store implementations
- [x] Publish the project to maven for external consumption

Membership
 - [x] Tests with members going up & down
 - [x] Ability to query any node and check for the available members

Partitioner
 - [x] Publish Partitioner trait
 - [x] Implement CH Partitioner
 - [x] forwardOrHandle based on Partitioner trait

Node Service
 - [x] gRPC <-> HTTP
 - [x] GET
 - [x] PUT
 - [x] HEALTH
 - [ ] SHARD_INFO (Good to have) - de-scoped - Tracked as part of [#20](https://github.com/ashwanthkumar/suuchi/issues/20)
  - what shards (key space)

Store Implementations
 - [x] InMemoryStore - Implement an in-memory store that support get and put
 - [x] RocksDB Store
