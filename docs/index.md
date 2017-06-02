# Suuchi

Having inspired from tools like [Uber's Ringpop](https://ringpop.readthedocs.io/) and a strong desire to understand how distributed systems work - Suuchi was born.

Suuchi is toolkit to build distributed data systems, that uses [gRPC](http://www.grpc.io/) under the hood as the communication medium. The overall goal of this project is to build pluggable components that can be easily composed by the developer to build a data system of desired characteristics.

> This project is in beta quality and it's currently running couple of systems in production setting [@indix](https://twitter.com/indix). We welcome all kinds of feedback to help improve the library.


### Latest versions

[![Suuchi Core on Maven Central](https://img.shields.io/maven-central/v/in.ashwanthkumar/suuchi-core.svg?label=suuchi-core&style=plastic)](https://maven-badges.herokuapp.com/maven-central/in.ashwanthkumar/suuchi-core)

[![Suuchi RocksDB on Maven Central](https://img.shields.io/maven-central/v/in.ashwanthkumar/suuchi-rocksdb.svg?label=suuchi-rocksdb&style=plastic)](https://maven-badges.herokuapp.com/maven-central/in.ashwanthkumar/suuchi-rocksdb)

[![Suuchi Atomix Cluster on Maven Central](https://img.shields.io/maven-central/v/in.ashwanthkumar/suuchi-cluster-atomix.svg?label=suuchi-cluster-atomix&style=plastic)](https://maven-badges.herokuapp.com/maven-central/in.ashwanthkumar/suuchi-cluster-atomix)

[![Suuchi Scalecube Cluster on Maven Central](https://img.shields.io/maven-central/v/in.ashwanthkumar/suuchi-cluster-scalecube.svg?label=suuchi-cluster-scalecube&style=plastic)](https://maven-badges.herokuapp.com/maven-central/in.ashwanthkumar/suuchi-cluster-scalecube)

### Dependencies
#### Maven
```xml
<dependency>
    <groupId>in.ashwanthkumar</groupId>
    <artifactId>suuchi-core</artifactId>
    <version>${suuchi.version}</version>
</dependency>
```

#### SBT
```sbt
libraryDependencies += "in.ashwanthkumar" % "suuchi-core" % suuchiVersion
```

Releases are published to [Sonatype release repository](https://oss.sonatype.org/content/repositories/releases) that eventually gets mirrored to Maven Central.

Development snapshots are available in [Sonatypes's snapshot repository](https://oss.sonatype.org/content/repositories/snapshots/).

## Features

- Enable partitioning of data using [Consistent Hashing](https://en.wikipedia.org/wiki/Consistent_hashing)
- Supports synchronous replication to desired number of nodes
- Enables above set of features for any gRPC based service definitions

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

## Presentations
Following presentations / videos explain motivation behind Suuchi

- [Suuchi - Distributed Data Systems Toolkit](https://speakerdeck.com/ashwanthkumar/suuchi-distributed-data-systems-toolkit/)
- [Suuchi - Application Layer Sharding](https://speakerdeck.com/ashwanthkumar/suuchi-application-layer-sharding)

## License
[https://www.apache.org/licenses/LICENSE-2.0](https://www.apache.org/licenses/LICENSE-2.0)
