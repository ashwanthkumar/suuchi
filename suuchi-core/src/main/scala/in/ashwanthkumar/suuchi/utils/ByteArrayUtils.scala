package in.ashwanthkumar.suuchi.utils

import java.util.{Arrays => JArrays}

object ByteArrayUtils {
  def hasPrefix(bytes: Array[Byte], prefix: Array[Byte]): Boolean = {
    bytes.length >= prefix.length && JArrays.equals(bytes.take(prefix.length), prefix)
  }
}
