package in.ashwanthkumar.suuchi.store

import in.ashwanthkumar.suuchi.utils.DateUtils

trait VersionedBy {
  val versionOrdering: Ordering[Long]
  def version(key: Array[Byte], value: Array[Byte]): Long

  /**
    * Choose either the versionTs or writtenTs for using it for purging. This is different
    * from [[VersionedBy#version]], only it it's way to store the actual version associated
    * with the key.
    *
    * @param version [[Version]] from which the implementation decides on what param it should sortOn
    * @return
    */
  @inline
  def sortOn(version: Version): Long = version.versionTs
}

class ByWriteTimestamp extends VersionedBy with DateUtils {
  override def version(key: Array[Byte], value: Array[Byte]): Long = now
  override val versionOrdering: Ordering[Long] = Ordering.Long.reverse
}
