package in.ashwanthkumar.suuchi.store.rocksdb

import java.io.File

import in.ashwanthkumar.suuchi.store.KV
import org.apache.commons.io.FileUtils
import org.scalatest.Matchers.{be, convertToAnyShouldWrapper, have, startWith}
import org.scalatest.{BeforeAndAfter, FlatSpec}

class RocksDbStoreSpec extends FlatSpec with BeforeAndAfter {

  val ROCKSDB_TEST_LOCATION = "/tmp/suuchi-rocks-test/"

  var db: RocksDbStore = _

  before {
    FileUtils.deleteDirectory(new File(ROCKSDB_TEST_LOCATION))
    db = new RocksDbStore(RocksDbConfiguration.apply(ROCKSDB_TEST_LOCATION))
  }

  after {
    FileUtils.deleteDirectory(new File(ROCKSDB_TEST_LOCATION))
    db.close()
  }

  "RocksDb" should "store & retrieve results properly" in {
    (1 to 100).foreach { i =>
      db.put(Array(i toByte), Array(i*2 toByte))
    }

    (1 to 100).foreach { i =>
      db.get(Array(i toByte)).get.head should be(i*2 toByte)
    }
  }

  it should "support full db scan" in {
    val inputKVs = (1 to 100).map(i => (Array(i toByte), Array(i*2 toByte)))

    inputKVs.foreach{case (k, v) => db.put(k, v)}
    val scannedResult = db.scan().toList

    scannedResult should have size 100
    scannedResult.sortBy(kv => new String(kv.key)) should be(inputKVs.map{case (k,v) => KV(k, v)}.toList)
  }

  it should "support prefix scan" in {
    val kVs = (1 to 100).flatMap(i => List((s"prefix1/$i".getBytes, Array(i toByte)), (s"prefix2/$i".getBytes, Array(i * 2 toByte))))

    kVs.foreach{case (k, v) => db.put(k, v)}
    val scannedResult = db.scan("prefix1".getBytes).toList

    scannedResult should have size 100
    scannedResult.foreach{ kv =>
      new String(kv.key) should startWith("prefix1")
    }
  }
}
