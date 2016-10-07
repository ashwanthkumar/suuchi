package in.ashwanthkumar.suuchi.store

import in.ashwanthkumar.suuchi.utils.DateUtils
import org.scalatest.FlatSpec
import org.scalatest.Matchers._

trait MockDateUtils extends DateUtils {
  var cnt = 0
  override def now: Long = {
    cnt += 1
    cnt
  }
}

class VersionedStoreSpec extends FlatSpec {
  "VersionedStore" should "return no version info for a key for the first time" in {
    val store = new VersionedStore(new InMemoryStore, 3)
    store.getVersions(Array(1.toByte)).size should be(0)
  }

  it should "return version info appropriately after every insert" in {
    val store = new VersionedStore(new InMemoryStore, 3) with MockDateUtils
    store.getVersions(Array(1.toByte)).size should be(0)

    store.put(Array(1.toByte), Array(100.toByte))
    store.getVersions(Array(1.toByte)) should be(List(1))

    store.put(Array(1.toByte), Array(101.toByte))
    store.getVersions(Array(1.toByte)) should be(List(2, 1))

    store.put(Array(1.toByte), Array(102.toByte))
    store.getVersions(Array(1.toByte)) should be(List(3, 2, 1))

    store.put(Array(1.toByte), Array(103.toByte))
    store.getVersions(Array(1.toByte)) should be(List(4,3,2))
  }

  it should "delete old versions of data for a key when we exceed numVersions" in {
    val inMemoryStore = new InMemoryStore
    val store = new VersionedStore(inMemoryStore, 3) with MockDateUtils
    store.getVersions(Array(1.toByte)).size should be(0)

    store.put(Array(1.toByte), Array(100.toByte))
    store.getVersions(Array(1.toByte)) should be(List(1))

    store.put(Array(1.toByte), Array(101.toByte))
    store.getVersions(Array(1.toByte)) should be(List(2, 1))

    store.put(Array(1.toByte), Array(102.toByte))
    store.getVersions(Array(1.toByte)) should be(List(3, 2, 1))

    store.put(Array(1.toByte), Array(103.toByte))
    inMemoryStore.get(VersionedStore.dkey(Array(1.toByte), 1)) should be(None)
  }
}
