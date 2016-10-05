# Suuchi
Toolkit to build distributed data systems.

## Library Dependency
### Maven
```xml
<dependency>
    <groupId>in.ashwanthkumar</groupId>
    <artifactId>suuchi-core</artifactId>
    <version>${suuchi.version}</version>
</dependency>
```

Development snapshots are available in [Sonatypes's snapshot repository](https://oss.sonatype.org/content/repositories/snapshots/).

If you are a developer looking to use Suuchi, head over to [Quick Start](quick-start.md) guide to get started.

## Recipes
- [Distributed InMemory Database](recipes/inmemorydb.md)
- [Distributed RocksDB backed KV](recipes/rocksdb.md)

## Internals
We try to document the internal workings of some core pieces of Suuchi for developers interested in contributing or understanding their systems better.

- [Partitioner](internals/partitioner.md)
- [Replication](internals/replication.md)
- [Router](internals/router.md)

## License
[https://www.apache.org/licenses/LICENSE-2.0](https://www.apache.org/licenses/LICENSE-2.0)
