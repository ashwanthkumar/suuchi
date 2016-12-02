package in.ashwanthkumar.suuchi.partitioner

import in.ashwanthkumar.suuchi.cluster.MemberAddress
import org.scalatest.FlatSpec
import org.scalatest.Matchers.{be, convertToAnyShouldWrapper}
import org.mockito.Mockito._

class RingStateSpec extends FlatSpec {

  "RingState" should "return true if a key is within start - end combination" in {
    val key: Array[Byte] = "key".getBytes()

    val mockHash = mock(classOf[Hash])
    RingState.contains(key, Int.MinValue, Int.MaxValue, SuuchiHash) should be(true)
    RingState.contains(key, -838523459, 0, SuuchiHash) should be(true)
    RingState.contains(key, -838523456, -838523458, SuuchiHash) should be(false)

    when(mockHash.hash(key)).thenReturn(1)
    RingState.contains(key, 1, 9, mockHash) should be(true)
    RingState.contains(key, 0, 10, mockHash) should be(true)
    RingState.contains(key, -10, 10, mockHash) should be(true)
    RingState.contains(key, -10, -1, mockHash) should be(false)
  }

  it should "return true if a key is within TokenRange" in {
    val key: Array[Byte] = "key".getBytes()
    RingState.contains(key, rangeOf(Int.MinValue, Int.MaxValue), SuuchiHash) should be(true)
    RingState.contains(key, rangeOf(-838523459, 0), SuuchiHash) should be(true)
  }

  def rangeOf(start: Int, end: Int): TokenRange = {
    TokenRange(start, end, VNode(MemberAddress("host1", 1), 1))
  }
}
