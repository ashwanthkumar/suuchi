# Partitioner

Partitioners are defined by the following

```scala
trait Partitioner {
  def find(key: Array[Byte], replicaCount: Int): List[MemberAddress]
}
```

An implementation of Partitioner is supposed to return the list of nodes where the given key should be placed if we need `replicaCount` number of replicas. 

Suuchi by default comes with a ConsistentHashPartitioner which uses ConsistentHashRing underneath to partition the data.

Interesting readings on Consistent Hash Ring

- [http://blog.plasmaconduit.com/consistent-hashing/](http://blog.plasmaconduit.com/consistent-hashing/)
- [http://www.paperplanes.de/2011/12/9/the-magic-of-consistent-hashing.html](http://www.paperplanes.de/2011/12/9/the-magic-of-consistent-hashing.html)

An example of CH Ring during assignment or replication.

<center>
 <img src="/images/internals/ch_ring.png" width="500" alt="CH Ring" />
</center>
