package in.ashwanthkumar.suuchi.store

import com.google.protobuf.ByteString
import org.scalatest.FlatSpec
import org.scalatest.Matchers.{be, convertToAnyShouldWrapper}

class InMemoryStoreTest extends FlatSpec {
  "InMemoryStore" should "support get and put on a KV" in {
    val store = new InMemoryStore
    store.put("1".getBytes, "2".getBytes) should be(true)
    store.get("1".getBytes).map(ByteString.copyFrom) should be(Some(ByteString.copyFrom("2".getBytes)))
  }
}
