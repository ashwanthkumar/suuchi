# Suuchi Release 0.1
What you get?
- Ability for nodes to join a cluster
- Ability to route traffic to different nodes based on CH strategy
- ability to write data and read data from the cluster

- Membership
 - Tests with members going up & down
 - Ability to query any node and check for the available members

- Partitioner
 - Publish Partitioner trait
 - Implement CH Partitioner
 - forwardOrHandle based on Partitioner trait

- Node Service
 - gRPC <-> HTTP
 - GET
 - PUT
 - HEALTH
 - SHARD_INFO (Good to have)
  - what shards (key space)

- Inmemory store
 - Implement an in-memory store that support get and put

