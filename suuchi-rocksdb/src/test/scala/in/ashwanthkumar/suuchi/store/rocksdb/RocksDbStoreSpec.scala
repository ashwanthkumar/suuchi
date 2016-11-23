package in.ashwanthkumar.suuchi.store.rocksdb

import java.io.File

import org.apache.commons.io.FileUtils
import org.scalatest.Matchers._
import org.scalatest.{BeforeAndAfter, FlatSpec}

class RocksDbStoreSpec extends FlatSpec with BeforeAndAfter {
  val ROCKSDB_TEST_LOCATION = "/tmp/suuchi-rocks-test"
  before {
    FileUtils.deleteDirectory(new File(ROCKSDB_TEST_LOCATION))
  }

  after {
    FileUtils.deleteDirectory(new File(ROCKSDB_TEST_LOCATION))
  }

  "RocksDb" should "store & retrieve results properly" in {
    val db = new RocksDbStore(RocksDbConfiguration.apply(ROCKSDB_TEST_LOCATION))
    (1 to 100).foreach { i =>
      db.put(Array(i toByte), Array(i*2 toByte))
    }

    (1 to 100).foreach { i =>
      db.get(Array(i toByte)).get.head should be(i*2 toByte)
    }

  }

  it should "scan values from store" in {
    val db = new RocksDbStore(RocksDbConfiguration.apply(ROCKSDB_TEST_LOCATION))
    (1 to 100).foreach { i =>
      db.put(Array(i toByte), Array(i*2 toByte))
    }

    val iterator = db.scan(Array(33 toByte), Array(42 toByte)).get

    iterator should have size 10

//    while (iterator.hasNext) {
//      println(new String(iterator.next()))
//    }
  }
}
