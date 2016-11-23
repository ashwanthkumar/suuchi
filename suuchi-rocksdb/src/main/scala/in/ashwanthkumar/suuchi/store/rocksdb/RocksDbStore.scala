package in.ashwanthkumar.suuchi.store.rocksdb

import in.ashwanthkumar.suuchi.store.Store
import in.ashwanthkumar.suuchi.utils.Logging
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

  override def scan(from: Array[Byte], to: Array[Byte]): Option[Iterator[Array[Byte]]] = {
    val rocksIterator = db.newIterator()
    rocksIterator.seek(from)

    val endOffset = new String(to)

    val iter = new Iterator[Array[Byte]] {
      override def hasNext: Boolean = rocksIterator.isValid && (new String(rocksIterator.key()) <= endOffset)

      override def next(): Array[Byte] = {
        val value = rocksIterator.value()
        println("val: " + new String(value))
        rocksIterator.next()
        value
      }
    }

    if (iter.isEmpty)
      return None
    Some(iter)
  }
}
