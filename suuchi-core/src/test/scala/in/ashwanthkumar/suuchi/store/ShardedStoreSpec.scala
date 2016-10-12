package in.ashwanthkumar.suuchi.store

import java.nio.ByteBuffer

import in.ashwanthkumar.suuchi.partitioner.Hash
import org.mockito.Mockito.{mock, times, verify, when}
import org.scalatest.FlatSpec
import org.scalatest.Matchers.{be, convertToAnyShouldWrapper}

class ShardedStoreSpec extends FlatSpec {
  "ShardedStore" should "create 2 stores for 2 different Partitions" in {
    val hash = mock(classOf[Hash])
    when(hash.hash("1".getBytes)).thenReturn(1)
    when(hash.hash("2".getBytes)).thenReturn(2)

    val store1 = mock(classOf[Store])
    when(store1.get("1".getBytes)).thenReturn(None)
    val store2 = mock(classOf[Store])
    when(store2.get("2".getBytes)).thenReturn(Some(Array(Byte.MaxValue)))

    val createStore = mock(classOf[(Int) => Store])
    when(createStore.apply(1)).thenReturn(store1)
    when(createStore.apply(2)).thenReturn(store2)

    val shardedStore = new ShardedStore(3, hash, createStore)
    val response = shardedStore.get("1".getBytes)
    verify(hash, times(1)).hash("1".getBytes)
    verify(createStore, times(1)).apply(1)
    verify(store1, times(1)).get("1".getBytes)
    response should be(None)

    val response2 = shardedStore.get("2".getBytes)
    verify(hash, times(1)).hash("2".getBytes)
    verify(createStore, times(1)).apply(2)
    verify(store2, times(1)).get("2".getBytes)
    response2.map(ByteBuffer.wrap) should be(Some(Array(Byte.MaxValue)).map(ByteBuffer.wrap))
  }

  it should "return the same store instance for the same partition but for different keys too" in {
    val hash = mock(classOf[Hash])
    when(hash.hash("1".getBytes)).thenReturn(1)
    when(hash.hash("2".getBytes)).thenReturn(1)

    val store = mock(classOf[Store])
    when(store.get("1".getBytes)).thenReturn(None)

    val createStore = mock(classOf[(Int) => Store])
    when(createStore.apply(1)).thenReturn(store)

    val shardedStore = new ShardedStore(3, hash, createStore)
    shardedStore.put("1".getBytes, "1".getBytes) should be(true)
    shardedStore.put("2".getBytes, "3".getBytes) should be(true)
    verify(store, times(1)).put("1".getBytes, "1".getBytes)
    verify(store, times(1)).put("2".getBytes, "3".getBytes)
  }

  it should "call get / put / remove on the delegate store when corresponding methods are called" in {
    val hash = mock(classOf[Hash])
    when(hash.hash("1".getBytes)).thenReturn(1)

    val store = mock(classOf[Store])
    when(store.get("1".getBytes)).thenReturn(None)
    when(store.put("1".getBytes, "2".getBytes)).thenReturn(true)
    when(store.remove("1".getBytes)).thenReturn(true)

    val createStore = mock(classOf[(Int) => Store])
    when(createStore.apply(1)).thenReturn(store)

    val shardedStore = new ShardedStore(3, hash, createStore)
    shardedStore.get("1".getBytes) should be(None)
    shardedStore.put("1".getBytes, "2".getBytes) should be(true)
    shardedStore.remove("1".getBytes) should be(true)

    verify(store, times(1)).get("1".getBytes)
    verify(store, times(1)).put("1".getBytes, "2".getBytes)
    verify(store, times(1)).remove("1".getBytes)
  }

  // TODO - Write tests for the synchronized {} in getStore.
}
