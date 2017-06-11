package in.ashwanthkumar.suuchi.store

import java.util

import com.google.common.io.ByteStreams
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

  def fromBytes(bytes: Array[Byte]): List[Version] = {
    val reader = ByteStreams.newDataInput(bytes)
    val numVersions = reader.readInt()
    (1 to numVersions).map {_ =>
      val versionTs = reader.readLong()
      val writtenTs = reader.readLong()
      Version(versionTs, writtenTs)
    }.toList
  }

  def toBytes(versions: List[Version]): Array[Byte] = {
    val writer = ByteStreams.newDataOutput()
    writer.writeInt(versions.size)
    versions.foreach {version =>
      writer.writeLong(version.versionTs)
      writer.writeLong(version.writtenTs)
    }
    writer.toByteArray
  }
}

object VersionedStore {
  val VERSION_PREFIX = "V_".getBytes
  val DATA_PREFIX    = "D_".getBytes

  def isVkeyKey(key: Array[Byte]) =
    util.Arrays.equals(key.take(VERSION_PREFIX.length), VERSION_PREFIX)
  def isDataKey(key: Array[Byte])                               = util.Arrays.equals(key.take(DATA_PREFIX.length), DATA_PREFIX)
  def vkey(key: Array[Byte])                                    = VERSION_PREFIX ++ key
  def dkey(key: Array[Byte]): Array[Byte]                       = DATA_PREFIX ++ key
  def dkey(key: Array[Byte], version: Array[Byte]): Array[Byte] = DATA_PREFIX ++ key ++ version
  def dkey(key: Array[Byte], version: Long): Array[Byte] =
    DATA_PREFIX ++ key ++ PrimitivesSerDeUtils.longToBytes(version)
}

case class Version(versionTs: Long, writtenTs: Long)
case class VRecord(key: Array[Byte], versions: List[Version]) {
  override def equals(obj: scala.Any): Boolean = obj match {
    case v: VRecord =>
      util.Arrays.equals(v.key, key) && versions.equals(v.versions)
    case _ => false
  }
  override def toString = s"VRecord(${util.Arrays.toString(key)},${versions.toString})"
}

class VersionedStore(store: Store,
                     versionedBy: VersionedBy,
                     numVersions: Int,
                     concurrencyFactor: Int = 8192)
    extends Store
    with DateUtils {

  import VersionedStore._

  val SYNC_SLOTS = Array.fill(concurrencyFactor)(new Object)

  override def get(key: Array[Byte]): Option[Array[Byte]] = {
    // fetch version record
    val vRecord = store.get(vkey(key))
    if (vRecord.isEmpty) None
    else {
      val versions = Versions.fromBytes(vRecord.get)
      get(key, versions.map(_.versionTs).max(versionedBy.versionOrdering))
    }
  }

  def get(key: Array[Byte], version: Long): Option[Array[Byte]] = {
    store.get(dkey(key, version))
  }

  override def put(key: Array[Byte], value: Array[Byte]): Boolean = {
    val currentVersion: Version = putAndPurgeVersions(key, value)
    putData(key, value, currentVersion)
  }

  override def remove(key: Array[Byte]) = {
    val versions = getVersions(key)
    removeVersion(key)
    versions.forall(v => removeData(key, v))
  }

  def getVersions(key: Array[Byte]): List[Version] = {
    val vRecord = store.get(vkey(key))
    vRecord
      .map(vr => Versions.fromBytes(vr))
      .getOrElse(List.empty[Version])
  }

  def putAndPurgeVersions(key: Array[Byte], value: Array[Byte], writtenTs: Long = now): Version = {
    val currentVersionTs = versionedBy.version(key, value)
    val currentVersion = Version(currentVersionTs, writtenTs)
    // atomically update version metadata
    val versions = atomicUpdate(key, currentVersion)
    // remove oldest version, if we've exceeded max # of versions per record
    if (versions.size > numVersions) removeData(key, versions.minBy(_.versionTs))

    currentVersion
  }

  def putData(key: Array[Byte], value: Array[Byte], currentVersion: Version): Boolean = {
    // Write out the actual data record
    store.put(dkey(key, currentVersion.versionTs), value)
  }

  private def atomicUpdate(key: Array[Byte], currentVersion: Version) = {
    val versionKey = vkey(key)
    val absHash    = math.abs(MurmurHash3.arrayHash(versionKey))
    // Synchronizing the version metadata update part alone
    val monitor = SYNC_SLOTS(absHash % SYNC_SLOTS.length)
    val versions = monitor.synchronized {
      val vRecord = store.get(versionKey)
      val updatedVersions = currentVersion :: vRecord
        .map(bytes => Versions.fromBytes(bytes))
        .getOrElse(List.empty[Version])
      store.put(
        versionKey,
        Versions.toBytes(updatedVersions.sortBy(_.versionTs)(versionedBy.versionOrdering).take(numVersions)))
      updatedVersions
    }
    versions
  }

  def toVRecord(kv: KV) = VRecord(kv.key.drop(VERSION_PREFIX.length), Versions.fromBytes(kv.value))

  override def scanner(): Scanner[KV] = new Scanner[KV] {

    private val delegate = store.scanner()

    override def prepare(): Unit                         = delegate.prepare()
    override def scan(prefix: Array[Byte]): Iterator[KV] = delegate.scan(dkey(prefix))
    override def scan(): Iterator[KV] =
      delegate.scan(DATA_PREFIX)
    override def close(): Unit = delegate.close()
  }

  def versionsScanner(): Scanner[VRecord] = new Scanner[VRecord] {
    private val delegate = store.scanner()

    override def prepare(): Unit = delegate.prepare()
    override def scan(prefix: Array[Byte]): Iterator[VRecord] =
      delegate.scan(vkey(prefix)).map(toVRecord)
    override def scan(): Iterator[VRecord] =
      delegate.scan(VERSION_PREFIX).map(toVRecord)
    override def close(): Unit = delegate.close()
  }

  private def removeData(key: Array[Byte], version: Version) = store.remove(dkey(key, version.versionTs))
  private def removeVersion(key: Array[Byte])             = store.remove(vkey(key))
}
