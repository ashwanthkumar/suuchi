# Suuchi Release 0.2
## Store
- [x] Sharded Store - #53
- [x] Versioned Store - #44
- [x] Scan support in all stores - #61, #58

## Replication
- [x] Pluggable replication - #45

## Routing
- [x] Aggregator Support - #65

## Cluster
- [x] Scalecube based clustering - #48
- [x] Refactored Atomix + Scalecube with ClusterProvider abstractions - #68

Atomix and scalecube modules need Java 8 to run.

## Bug Fixes
- [x] Opening all stores in ShardedStore if they're not open already - #64
- [x] Adding default of 10 minutes in replication, aggregation etc. - #62, #65

## Operations
- [x] Moved away from SnapCI to Travis based build

List of all the changes - https://github.com/ashwanthkumar/suuchi/pulls?q=is%3Apr+is%3Aclosed+milestone%3A0.2

## Contributors
- [Ashwanth Kumar](https://github.com/ashwanthkumar)
- [Sriram R](https://github.com/brewkode)
- [Selvaram Ganesh](https://github.com/gsriram7)
