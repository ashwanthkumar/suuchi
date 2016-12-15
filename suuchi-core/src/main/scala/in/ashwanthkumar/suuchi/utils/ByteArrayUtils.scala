package in.ashwanthkumar.suuchi.utils

import java.util.{Arrays => JArrays}

import in.ashwanthkumar.suuchi.partitioner.Hash

object ByteArrayUtils {
  def hasPrefix(bytes: Array[Byte], prefix: Array[Byte]): Boolean = {
    bytes.length >= prefix.length && JArrays.equals(bytes.take(prefix.length), prefix)
  }

  def isHashKeyWithinRange(start: Int, end: Int, key: Array[Byte], hashFn: Hash) = {
    val hash = hashFn.hash(key)

    start <= hash && hash <= end
  }
}
