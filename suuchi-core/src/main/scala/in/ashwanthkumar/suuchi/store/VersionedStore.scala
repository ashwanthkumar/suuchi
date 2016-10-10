package in.ashwanthkumar.suuchi.store

import in.ashwanthkumar.suuchi.utils.DateUtils
import scala.util.hashing.MurmurHash3

import scala.language.postfixOps


object Versions {
  /*
  *
  * Serialization util for managing list of Versions of type Long
  * We follow the below protocol to serialize List[Long] to Array[Byte]
  *
  * First byte - # of elements in the list - N
  * We will have N "longs represented as bytes - 8 bytes each" following it
  * Since, we know each version is a long, we just read 8 bytes to construct a long and move forward.
  * */

  def fromBytes(bytes: Array[Byte]): List[Long] = {
    val numVersions = bytes(0).toInt
    bytes.drop(1).sliding(8, 8).map(PrimitivesSerDeUtils.bytesToLong) toList
  }

  def toBytes(versions: List[Long]): Array[Byte] = {
    val numVersions = versions.size.toByte
    val bytes = versions.flatMap(PrimitivesSerDeUtils.longToBytes)
    numVersions :: bytes toArray
  }
}

object VersionedStore {
  val VERSION_PREFIX = "V_".getBytes
  val DATA_PREFIX = "D_".getBytes

  def vkey(key: Array[Byte]) = VERSION_PREFIX ++ key
  def dkey(key: Array[Byte], version: Array[Byte]): Array[Byte] = DATA_PREFIX ++ key ++ version
  def dkey(key: Array[Byte], version: Long): Array[Byte] = DATA_PREFIX ++ key ++ PrimitivesSerDeUtils.longToBytes(version)
}

class VersionedStore(store: Store, versionedBy: VersionedBy, numVersions: Int, concurrencyFactor: Int = 8192) extends Store with DateUtils {

  import VersionedStore._

  val SYNC_SLOTS = Array.fill(concurrencyFactor)(new Object)

  override def get(key: Array[Byte]): Option[Array[Byte]] = {
    // fetch version record
    val vRecord = store.get(vkey(key))
    if (vRecord.isEmpty) None
    else {
      val versions = Versions.fromBytes(vRecord.get)
      get(key, versions.max(versionedBy.versionOrdering))
    }
  }

  def get(key: Array[Byte], version: Long): Option[Array[Byte]] = {
    store.get(dkey(key, version))
  }

  override def put(key: Array[Byte], value: Array[Byte]): Boolean = {
    val currentVersion = versionedBy.version(key, value)

    // atomically update version metadata
    val versions = atomicUpdate(key, currentVersion)

    // remove oldest version, if we've exceeded max # of versions per record
    if (versions.size > numVersions) removeData(key, versions.min)

    // Write out the actual data record
    store.put(dkey(key, currentVersion), value)

  }

  override def remove(key: Array[Byte]) = {
    val versions = getVersions(key)
    removeVersion(key)
    versions.forall(v => removeData(key, v))
  }

  def getVersions(key: Array[Byte]): List[Long] = {
    val vRecord = store.get(vkey(key))
    vRecord
      .map(vr => Versions.fromBytes(vr))
      .getOrElse(List.empty[Long])
  }

  private def atomicUpdate(key: Array[Byte], version: Long) = {
    val versionKey = vkey(key)
    val absHash = math.abs(MurmurHash3.bytesHash(versionKey))
    // Synchronizing the version metadata update part alone
    val monitor = SYNC_SLOTS(absHash % SYNC_SLOTS.length)
    val versions = monitor.synchronized {
      val vRecord = store.get(versionKey)
      val updatedVersions = version :: vRecord.map(bytes => Versions.fromBytes(bytes)).getOrElse(List.empty[Long])
      store.put(versionKey, Versions.toBytes(updatedVersions.sorted(versionedBy.versionOrdering).take(numVersions)))
      updatedVersions
    }
    versions
  }

  private def removeData(key: Array[Byte], version: Long) = store.remove(dkey(key, version))
  private def removeVersion(key: Array[Byte]) = store.remove(vkey(key))
}
