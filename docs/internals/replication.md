# Replication Internals

Suuchi out of the box comes with _Synchronous Sequential Replication_ during writes. It's fairly easy to build custom replicators.

Refer [#27](https://github.com/ashwanthkumar/suuchi/pull/27) and [#23](https://github.com/ashwanthkumar/suuchi/pull/23) on how Replication is implemented.

## Types
- Synchronous SequentialReplication (default available)
- Synchronous ParallelReplication - (default available)
- Synchronous ChainedReplication - ([#31](https://github.com/ashwanthkumar/suuchi/issues/31))

## Working with Methods that handle Replication
When implementing a service sometimes as a developer we would like to know if a particular invocation is a replication invocation or not. May be you might want to send a metric for every write and don't want to send the same metric multiple times. Sometimes you would also want to do a specific operation only at the primary shard and not others. You can use the following snippet

```scala
class WriteService extends ... { 
  def write(...) = {
    if(Headers.PRIMARY_NODE_REQUEST_CTX.get()) {
        // do something if this is a primary replica
    }
  }
}
```
