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
  val families = mutable.Map[String, ColumnFamilyHandle]()

  lazy val db = {
    if (config.readOnly) RocksDB.openReadOnly(config.toOptions, config.location)
    else {
      val columnFamilies = Option(RocksDB.listColumnFamilies(config.toOptions, config.location)).getOrElse(new util.ArrayList[Array[Byte]]())
        .map(bytes => new String(bytes))
      val descriptors = columnFamilies.map(family => new ColumnFamilyDescriptor(family.getBytes()))
      descriptors.add(new ColumnFamilyDescriptor("default".getBytes()))
      val handles = new util.ArrayList[ColumnFamilyHandle]()
      val db: RocksDB = RocksDB.open(config.asDBOptions, config.location, descriptors, handles)
      0.to(columnFamilies.size-1).foreach{ i => families.put(columnFamilies(i), handles(i))}
      db
    }
  }
  RocksDB.loadLibrary()
  lazy val writeOptions = new WriteOptions().setDisableWAL(true).setSync(true)

  val DEFAULT_FAMILY =  db.getDefaultColumnFamily

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

  def createColumnFamilyHandle(familyName:String):ColumnFamilyHandle = this.synchronized {
    if(families contains familyName) return families(familyName)
    val descriptor = new ColumnFamilyDescriptor(familyName.getBytes)
    val newColumnFamily = db.createColumnFamily(descriptor)
    families.put(familyName, newColumnFamily)
    newColumnFamily
  }
}