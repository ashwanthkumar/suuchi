package in.ashwanthkumar.suuchi.store.rocksdb

import java.util.{Arrays => JArrays}

import in.ashwanthkumar.suuchi.store.{KV, Store}
import in.ashwanthkumar.suuchi.utils.{ByteArrayUtils, Logging}
import org.rocksdb._

import scala.language.postfixOps

class RocksDbStore(config: RocksDbConfiguration) extends Store with Logging {
  lazy val db = {
    if (config.readOnly) RocksDB.openReadOnly(config.toOptions, config.location)
    else RocksDB.open(config.toOptions, config.location)
  }

  RocksDB.loadLibrary()
  lazy val writeOptions = new WriteOptions().setDisableWAL(false).setSync(true)

  override def get(key: Array[Byte]) = this.synchronized {
    Option(db.get(key))
  }

  override def put(key: Array[Byte], value: Array[Byte]) = this.synchronized {
    logOnError(() => db.put(writeOptions, key, value)) isSuccess
  }

  def close() = {
    log.info(s"[Closing RocksDb]")
    db.close()
  }

  override def remove(key: Array[Byte]): Boolean = {
    logOnError(() => db.remove(key)) isSuccess
  }

  def scan(): Iterator[KV] = scan(Array.ofDim[Byte](0))

  def scan(prefix: Array[Byte]): Iterator[KV] = {
    val rocksIterator: RocksIterator = db.newIterator()
    rocksIterator.seek(prefix)

    new Iterator[KV] {
      override def hasNext: Boolean = rocksIterator.isValid && ByteArrayUtils.hasPrefix(rocksIterator.key(), prefix)

      override def next(): KV = {
        val kv = KV(rocksIterator.key(), rocksIterator.value())
        rocksIterator.next()
        kv
      }
    }
  }

}
