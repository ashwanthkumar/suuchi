package in.ashwanthkumar.suuchi.utils

import org.scalatest.FlatSpec
import org.scalatest.Matchers._

class ByteArrayUtilsSpec extends FlatSpec {

  "ByteArrayUtils" should "say whether a given byte array starts with a specified byte array prefix" in {
    ByteArrayUtils.hasPrefix("string".getBytes, prefix = "char".getBytes) should be(false)
    ByteArrayUtils.hasPrefix("string".getBytes, prefix = "str".getBytes) should be(true)
    ByteArrayUtils.hasPrefix("string".getBytes, prefix = "string".getBytes) should be(true)
  }

  it should "return false when the given prefix is longer than the key" in {
    ByteArrayUtils.hasPrefix("string".getBytes, prefix = "longerString".getBytes) should be(false)
  }

}
