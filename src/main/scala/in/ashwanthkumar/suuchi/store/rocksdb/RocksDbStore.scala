package in.ashwanthkumar.suuchi.store.rocksdb

import java.util

import in.ashwanthkumar.suuchi.store.Store
import in.ashwanthkumar.suuchi.utils.Logging
import org.rocksdb._
import org.slf4j.LoggerFactory
import scala.collection.mutable
import scala.collection.JavaConversions._
import scala.util.Try

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
    Try(db.put(writeOptions, key, value)).isSuccess
  }

  def close() = {
    log.info(s"[Closing RocksDb]")
    db.close()
  }
}