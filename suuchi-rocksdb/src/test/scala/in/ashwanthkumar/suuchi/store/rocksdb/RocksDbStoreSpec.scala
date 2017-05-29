package in.ashwanthkumar.suuchi.store.rocksdb

import java.nio.file.Files
import java.util.UUID

import in.ashwanthkumar.suuchi.store.{KV, StoreUtils}
import org.apache.commons.io.FileUtils
import org.scalatest.Matchers.{be, convertToAnyShouldWrapper, have, startWith}
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FlatSpec}

class RocksDbStoreSpec extends FlatSpec with BeforeAndAfter with BeforeAndAfterAll {

  val dir = Files.createTempDirectory("suuchi-rocks-test").toFile

  override def afterAll() = {
    FileUtils.deleteDirectory(dir.getAbsoluteFile)
  }

  "RocksDb" should "store & retrieve results properly" in {
    val db = createDB()
    (1 to 100).foreach { i =>
      db.put(Array(i toByte), Array(i*2 toByte))
    }

    (1 to 100).foreach { i =>
      db.get(Array(i toByte)).get.head should be(i*2 toByte)
    }
  }

  it should "support full db scan" in {
    val db = createDB()
    val inputKVs = (1 to 100).map(i => (Array(i toByte), Array(i*2 toByte)))

    inputKVs.foreach{case (k, v) => db.put(k, v)}
    val scannedResult = StoreUtils.scan(db.scanner()).toList

    scannedResult should have size 100
    scannedResult.sortBy(kv => new String(kv.key)) should be(inputKVs.map{case (k,v) => KV(k, v)}.toList)
  }

  it should "support prefix scan" in {
    val db = createDB()
    val kVs = (1 to 100).flatMap(i => List((s"prefix1/$i".getBytes, Array(i toByte)), (s"prefix2/$i".getBytes, Array(i * 2 toByte))))

    kVs.foreach{case (k, v) => db.put(k, v)}
    val scannedResult = StoreUtils.scan("prefix1".getBytes, db.scanner()).toList

    scannedResult should have size 100
    scannedResult.foreach{ kv =>
      new String(kv.key) should startWith("prefix1")
    }
  }

  def createDB() = {
    val location = dir.getAbsolutePath + "/" + UUID.randomUUID()
    new RocksDbStore(RocksDbConfiguration(location))
  }
}
