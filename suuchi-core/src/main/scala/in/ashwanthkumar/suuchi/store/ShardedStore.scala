package in.ashwanthkumar.suuchi.store

import java.util.concurrent.ConcurrentHashMap

import in.ashwanthkumar.suuchi.partitioner.Hash
import in.ashwanthkumar.suuchi.utils.Logging

/**
 * SharedStore shards the keys equally into [[partitionsPerNode]] stores and proxies store operations
 * against them for a given key.
 * <p/>
 *
 * DO NOT CHANGE THE [[hashFn]] and [[partitionsPerNode]] on an existing store,
 * we wouldn't be able to read previously hashed keys.
 *
 * TODO - May be build a tool that can migrate between the shards (hashes and number)
 * by doing a full scan of the underlying store and re-creating the data.
 *
 * @param partitionsPerNode Number of stores we need to shard the data into
 * @param hashFn  HashFunction used to compute the shard
 * @param createStore  Function to return a store instance given a partitionId.
 *                     This would be created in the form [1 -> partitionsPerNode].
 *                     Take care to not throw exceptions in this method. If it does,
 *                     we propagate that error back to the service who invoked us.
 */
class ShardedStore(partitionsPerNode: Int, hashFn: Hash, createStore: (Int) => Store) extends Store with Logging {
  private val map = new ConcurrentHashMap[Integer, Store](partitionsPerNode)

  private val locks = Array.fill(partitionsPerNode)(new Object)

  override def get(key: Array[Byte]): Option[Array[Byte]] = logOnError(() => getStore(key).get(key)).getOrElse(None)
  override def put(key: Array[Byte], value: Array[Byte]): Boolean = logOnError(() => getStore(key).put(key, value)).isSuccess
  override def remove(key: Array[Byte]): Boolean = logOnError(() => getStore(key).remove(key)).isSuccess

  private def getStore(key: Array[Byte]): Store = {
    val partition = math.abs(hashFn.hash(key)) % partitionsPerNode
    if (map.containsKey(partition)) {
      map.get(partition)
    } else {
      locks(partition).synchronized {
        // To trade off between locks for most of the code path and consistency in not invoking `createStore`
        // unless absolutely we need it.
        if (!map.containsKey(partition)) {
          val store = logOnError(() => createStore(partition)).get
          map.put(partition, store)
          store
        } else {
          map.get(partition)
        }
      }
    }
  }
}
