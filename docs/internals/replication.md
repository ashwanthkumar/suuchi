# Replication Internals

Suuchi out of the box comes with _Synchronous Sequential Replication_ during writes. It's fairly easy to build custom replicators.

Refer [#27](https://github.com/ashwanthkumar/suuchi/pull/27) and [#23](https://github.com/ashwanthkumar/suuchi/pull/23) on how Replication is implemented.

## Types
- Synchronous SequentialReplication (default available)
- Synchronous ParallelReplication - ([#30](https://github.com/ashwanthkumar/suuchi/issues/30))
- Synchronous ChainedReplication - ([#31](https://github.com/ashwanthkumar/suuchi/issues/31))
