package in.ashwanthkumar.suuchi.store

import java.nio.ByteBuffer

object PrimitivesSerDeUtils {
  /*
  * FIXME: Not the most effective way to perform serde primitives.
  * */
  def longToBytes(instance: Long) = ByteBuffer.allocate(8).putLong(instance).array()
  def intToBytes(instance: Int) = ByteBuffer.allocate(4).putInt(instance).array()
  def bytesToInt(bytes: Array[Byte]) = ByteBuffer.wrap(bytes).getInt
  def bytesToLong(bytes: Array[Byte]) = ByteBuffer.wrap(bytes).getLong
}
