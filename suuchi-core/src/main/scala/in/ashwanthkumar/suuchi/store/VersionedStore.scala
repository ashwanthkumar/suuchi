package in.ashwanthkumar.suuchi.store

import in.ashwanthkumar.suuchi.utils.DateUtils
import scala.util.hashing.MurmurHash3


object Versions {
  def fromBytes(bytes: Array[Byte]) = fromString(new String(bytes))
  def fromString(versionString: String) = versionString.split('|').map(_.toLong)
  def toString(versions: List[Long]) = versions.map(_.toString).mkString("|")
  def toBytes(versions: List[Long]) = toString(versions).getBytes
}

object VersionedStore {
  val VERSION_PREFIX = "V_".getBytes()
  val DATA_PREFIX = "D_".getBytes()
  def vkey(key: Array[Byte]) = VERSION_PREFIX ++ key
  def dkey(key: Array[Byte], version: Long) = DATA_PREFIX ++ key ++ version.toString.getBytes
}

class VersionedStore(store: Store, numVersions: Int, concurrencyFactor: Int = 8192) extends Store with DateUtils {
  import VersionedStore._
  val SYNC_SLOTS = Array.fill(concurrencyFactor)(new Object)

  override def get(key: Array[Byte]) : Option[Array[Byte]] = {
    // fetch version record
    val vRecord = store.get(vkey(key))
    if(vRecord.isEmpty) None
    else {
      val versions = Versions.fromBytes(vRecord.get)
      get(key, versions.max)
    }
  }

  def get(key: Array[Byte], version: Long) : Option[Array[Byte]] = {
    store.get(dkey(key, version))
  }

  override def put(key: Array[Byte], value: Array[Byte]): Boolean = {
    val nowMs = now

    // atomically update version metadata
    val versions = atomicUpdate(key, nowMs)

    // remove oldest version, if we've exceeded max # of versions per record
    if(versions.size > numVersions) removeData(key, versions.min)

    // Write out the actual data record
    store.put(dkey(key, versions.max), value)

  }

  def atomicUpdate(key: Array[Byte], version: Long) = {
    val versionKey = vkey(key)
    val absHash = math.abs(MurmurHash3.bytesHash(versionKey))
    // Synchronizing the version metadata update part alone
    val monitor = SYNC_SLOTS(absHash % SYNC_SLOTS.length)
    val versions  = monitor.synchronized {
      val vRecord = store.get(versionKey)
      val updatedVersions = version :: vRecord.map(bytes => Versions.fromBytes(bytes).toList).getOrElse(List.empty[Long])
      store.put(versionKey, Versions.toBytes(updatedVersions.take(numVersions)))
      updatedVersions
    }
    versions
  }

  def remove(key: Array[Byte]) = {
    val versions = getVersions(key)
    removeVersion(key)
    versions.forall(v => removeData(key, v))
  }

  private def removeData(key: Array[Byte], version: Long) = store.remove(dkey(key, version))
  private def removeVersion(key: Array[Byte]) = store.remove(vkey(key))

  private[store] def getVersions(key: Array[Byte]): List[Long] = {
    val vRecord = store.get(vkey(key))
    vRecord
      .map(vr => Versions.fromBytes(vr).toList)
      .getOrElse(List.empty[Long])
  }
}



