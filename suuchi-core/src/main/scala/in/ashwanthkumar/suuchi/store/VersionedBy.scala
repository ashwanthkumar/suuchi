package in.ashwanthkumar.suuchi.store


import in.ashwanthkumar.suuchi.utils.DateUtils

trait VersionedBy {
  val versionOrdering: Ordering[Long]
  def version(key: Array[Byte], value: Array[Byte]) : Long
}

class ByWriteTimestamp extends VersionedBy with DateUtils {
  override def version(key: Array[Byte], value: Array[Byte]): Long = now
  override val versionOrdering: Ordering[Long] = Ordering.Long.reverse
}

