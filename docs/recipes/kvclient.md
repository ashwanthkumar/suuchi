# Distributed KVClient

In the either of [InMemory KV Store](inmemorydb.md) or [RocksDB based KV Store](rocksdb.md) recipes we only started a server. We need some way to access the server. This recipe is about a simple gRPC client that does a PUT followed by a GET of the same key in the cluster and verify that they're the same.

```
package in.ashwanthkumar.suuchi

import java.nio.ByteBuffer

import in.ashwanthkumar.suuchi.client.SuuchiClient

object DistributedKVClient extends App {
  val client = new SuuchiClient("localhost", 5051)
  val putResponse = client.put(Array(65.toByte), Array(65.toByte)) // puts k=v as A=A (in bytes)
  require(putResponse, "client should have responded successfully")

  val getResponse = client.get(Array(65.toByte))
  require(getResponse.isDefined, "server should return a valid response") // gets k=A
  require(ByteBuffer.wrap(getResponse.get) == ByteBuffer.wrap(Array(65.toByte)), "response seems invalid - it should be A")

  println("Client has been validated")
}
```

While running with this client you can find information about how the node in the point of contact with the client automatically
- forwarded requests to the right node based on Input Key and ConsistentHash Ring
- replicates the given message across multiple nodes again based on ConsistentHashing Ring.
