package in.ashwanthkumar.suuchi.store.rocksdb

import in.ashwanthkumar.suuchi.store.{KV, Scanner, Store}
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

  override def scanner(): Scanner[KV] = new RocksDBScanner(db)
}

class RocksDBScanner(db: RocksDB) extends Scanner[KV] {

  private[this] lazy val snapshot = db.getSnapshot
  private[this] var rocksIterator: RocksIterator = _

  override def prepare(): Unit = {
    rocksIterator = db.newIterator()
  }

  override def scan(prefix: Array[Byte]): Iterator[KV] = {
    rocksIterator.seek(prefix)

    new Iterator[KV] {
      override def hasNext: Boolean =
        rocksIterator.isValid && ByteArrayUtils.hasPrefix(rocksIterator.key(), prefix)

      override def next(): KV = {
        val kv = KV(rocksIterator.key(), rocksIterator.value())
        rocksIterator.next()
        kv
      }
    }
  }

  override def scan(): Iterator[KV] = scan(Array.ofDim[Byte](0))

  override def close(): Unit = {
    rocksIterator.close()
    db.releaseSnapshot(snapshot)
    snapshot.close()
  }

}
