# Suuchi
Toolkit to build distributed data systems.

Current version of Suuchi is `"0.1.0"`.

### Maven
```xml
<dependency>
    <groupId>in.ashwanthkumar</groupId>
    <artifactId>suuchi-core</artifactId>
    <version>${suuchi.version}</version>
</dependency>
```

### SBT
```sbt
libraryDependencies += "in.ashwanthkumar" % "suuchi-core" % suuchiVersion
```

Releases are published to [Sonatype release repository](https://oss.sonatype.org/content/repositories/releases) that eventually gets mirrored to Maven Central.

Development snapshots are available in [Sonatypes's snapshot repository](https://oss.sonatype.org/content/repositories/snapshots/).

If you are a developer looking to use Suuchi, head over to [Quick Start](quick-start.md) guide to get started.

## Recipes
- [Distributed InMemory Database](recipes/inmemorydb.md)
- [Distributed RocksDB backed KV](recipes/rocksdb.md)
- [Distributed KVClient](recipes/kvclient.md)

## Internals
We try to document the internal workings of some core pieces of Suuchi for developers interested in contributing or understanding their systems better.

- [Partitioner](internals/partitioner.md)
- [Replication](internals/replication.md)
- [Router](internals/router.md)

## License
[https://www.apache.org/licenses/LICENSE-2.0](https://www.apache.org/licenses/LICENSE-2.0)
