package in.ashwanthkumar.suuchi.utils

import in.ashwanthkumar.suuchi.partitioner.{Hash, SuuchiHash}
import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import org.mockito.Mockito._

class ByteArrayUtilsSpec extends FlatSpec {

  "ByteArrayUtils" should "say whether a given byte array starts with a specified byte array prefix" in {
    ByteArrayUtils.hasPrefix("string".getBytes, prefix = "char".getBytes) should be(false)
    ByteArrayUtils.hasPrefix("string".getBytes, prefix = "str".getBytes) should be(true)
    ByteArrayUtils.hasPrefix("string".getBytes, prefix = "string".getBytes) should be(true)
  }

  it should "return false when the given prefix is longer than the key" in {
    ByteArrayUtils.hasPrefix("string".getBytes, prefix = "longerString".getBytes) should be(false)
  }

  it should "return true if hash of the given key within start, end range" in {
    val hashFn = mock(classOf[Hash])
    val key = "1".getBytes

    when(hashFn.hash(key)).thenReturn(10)

    ByteArrayUtils.isHashKeyWithinRange(1, 50, key, hashFn) should be(true)
  }

  it should "return false if hash of the given key is not within start, end range" in {
    val hashFn = mock(classOf[Hash])
    val key = "1".getBytes

    when(hashFn.hash(key)).thenReturn(1).thenReturn(100)

    ByteArrayUtils.isHashKeyWithinRange(10, 100, key, hashFn) should be(false)
    ByteArrayUtils.isHashKeyWithinRange(1, 10, key, hashFn) should be(false)
  }

}
