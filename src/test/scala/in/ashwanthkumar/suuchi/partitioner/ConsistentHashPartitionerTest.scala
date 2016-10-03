package in.ashwanthkumar.suuchi.partitioner

import in.ashwanthkumar.suuchi.membership.MemberAddress
import org.scalatest.FlatSpec
import org.scalatest.Matchers.{convertToAnyShouldWrapper, be}

class ConsistentHashPartitionerTest extends FlatSpec {

  "ConsistentHashPartitioner" should "not return anything when underlying CHR has 0 nodes" in {
    val partitioner = ConsistentHashPartitioner(Nil)
    partitioner.find("1".getBytes) should be(Nil)
  }

  it should "return a node when underlying CHR has a node" in {
    val partitioner = ConsistentHashPartitioner(ConsistentHashRing(1).add(MemberAddress("host1", 1)))
    partitioner.find("1".getBytes) should be(List(VNode(MemberAddress("host1", 1), 1)))
  }

}
