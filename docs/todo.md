# Suuchi Release 0.1
What you get?
- [x] Ability for nodes to join a cluster
- [ ] Ability to route traffic to different nodes based on CH strategy
- [ ] Ability to write data and read data from the cluster

- Membership
 - [x] Tests with members going up & down
 - [x] Ability to query any node and check for the available members

- Partitioner
 - [ ] Publish Partitioner trait
 - [ ] Implement CH Partitioner
 - [ ] forwardOrHandle based on Partitioner trait

- Node Service
 - [x] gRPC <-> HTTP
 - [x] GET
 - [x] PUT
 - [x] HEALTH
 - [ ] SHARD_INFO (Good to have)
  - what shards (key space)

- [x] InMemoryStore
 - Implement an in-memory store that support get and put
