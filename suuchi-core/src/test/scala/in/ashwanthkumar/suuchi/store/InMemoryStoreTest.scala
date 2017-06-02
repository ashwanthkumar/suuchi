package in.ashwanthkumar.suuchi.store

import org.scalatest.FlatSpec
import org.scalatest.Matchers.{be, convertToAnyShouldWrapper, have, startWith}

class InMemoryStoreTest extends FlatSpec {
  "InMemoryStore" should "support get and put on a KV" in {
    val store = new InMemoryStore
    store.put("1".getBytes, "2".getBytes) should be(true)
    store.get("1".getBytes).map(v => new String(v)) should be(Some("2"))
  }

  it should "support full store scan" in {
    val store = new InMemoryStore
    store.put("1".getBytes, "one".getBytes)
    store.put("2".getBytes, "two".getBytes)
    store.put("3".getBytes, "three".getBytes)
    store.put("4".getBytes, "four".getBytes)
    store.put("5".getBytes, "five".getBytes)

    val kVs = store.scan().toList
    kVs should have size 5
    kVs.sortBy(kv => new String(kv.key)) should be(
      List(kv("1", "one"), kv("2", "two"), kv("3", "three"), kv("4", "four"), kv("5", "five")))
  }

  it should "support prefix scan" in {
    val store = new InMemoryStore
    store.put("prefix1/1".getBytes, "one".getBytes)
    store.put("prefix1/2".getBytes, "two".getBytes)
    store.put("prefix1/3".getBytes, "three".getBytes)
    store.put("prefix2/1".getBytes, "eleven".getBytes)
    store.put("prefix2/2".getBytes, "twelve".getBytes)
    store.put("prefix2/3".getBytes, "thirteen".getBytes)

    val kVs = store.scan("prefix1".getBytes).toList

    kVs.foreach { kv =>
      new String(kv.key) should startWith("prefix1")
    }
  }

  def kv(key: String, value: String) = KV(key.getBytes, value.getBytes)
}
