package in.ashwanthkumar.suuchi.store

import org.scalatest.FlatSpec
import org.scalatest.Matchers.{be, convertToAnyShouldWrapper}

class InMemoryStoreTest extends FlatSpec {
  "InMemoryStore" should "support get and put on a KV" in {
    val store = new InMemoryStore
    store.put("1".getBytes, "2".getBytes) should be(true)
    store.get("1".getBytes).map(v => new String(v)) should be(Some("2"))
  }

/*  it should "support scan operation for a given range of keys" in {
    val store = new InMemoryStore
    var idx = 1
    "abcdefg".toCharArray.toIterable.foreach(c =>{
      store.put(c.toString.getBytes, idx.toString.getBytes)
      idx = idx + 1
    })

    val maybeIterator = store.scan("c".getBytes(), "e".getBytes())
    val iterator = maybeIterator.get

    iterator.toList.map(new String(_)).foreach(println)

  }*/

}
