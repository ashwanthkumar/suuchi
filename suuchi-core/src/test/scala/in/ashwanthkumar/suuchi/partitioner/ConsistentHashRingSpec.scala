package in.ashwanthkumar.suuchi.partitioner

import in.ashwanthkumar.suuchi.membership.MemberAddress
import org.scalatest.FlatSpec
import org.scalatest.Matchers._

object IdentityHash extends Hash {
  override def hash(bytes: Array[Byte]): Integer = bytes.toString.toInt
}

class ConsistentHashRingSpec extends FlatSpec {
  "ConsistentHashRing" should "pin nodes into the ring accounting for virtual nodes" in {
    val nodes = List(MemberAddress("host1", 1), MemberAddress("host2", 2), MemberAddress("host3", 3))
    val ring = ConsistentHashRing(nodes, 3)

    ring.nodes.size() should be(9)

    ring.add(MemberAddress("host100", 100))
    ring.nodes.size() should be(12)
  }

  it should "remove nodes & its replica nodes on remove" in {
    val ring = new ConsistentHashRing(SuuchiHash, 3)
    ring.init(List(MemberAddress("host1", 1), MemberAddress("host2", 2), MemberAddress("host3", 3)))

    ring.remove(MemberAddress("host1", 1))
    ring.nodes.size should be(6)

    ring.remove(MemberAddress("host2", 2))
    ring.nodes.size should be(3)

    ring.remove(MemberAddress("host3", 3))
    ring.nodes.size should be(0)

    ring.remove(MemberAddress("host4", 4))
    ring.nodes.size should be(0)
  }

  it should "return None when no nodes are present in the ring" in {
    val ring = ConsistentHashRing(Nil, 3)
    ring.find("1".getBytes) should be(None)
  }

  it should "return the only node on find when only 1 node is present in the ring" in {
    val ring = ConsistentHashRing(List(MemberAddress("host1", 1)), 1)
    ring.find("1".getBytes) should be(Some(MemberAddress("host1", 1)))
  }

  it should "return the same node multiple times when the number of unique nodes is less but requested bins are more" in {
    val ring = ConsistentHashRing(List(MemberAddress("host1", 1)), 3)
    val list = ring.find("1".getBytes, 3)
    list should have size 3
    list.head should be(MemberAddress("host1", 1))
    list(1) should be(MemberAddress("host1", 1))
    list(2) should be(MemberAddress("host1", 1))
  }

  it should "might return same node multiple times even when we have enough number of nodes" in {
    val members = (1 to 5).map { index => MemberAddress(s"host$index", index) }.toList
    val ring = ConsistentHashRing(members, 3)
    val list = ring.find("1".getBytes, 3)
    list should have size 3
    list.head should be(MemberAddress("host3", 3))
    list(1) should be(MemberAddress("host3", 3))
    list(2) should be(MemberAddress("host4", 4))
  }

  it should "not return the same node multiple times" in {
    val ring = ConsistentHashRing(List(MemberAddress("host1", 1)), 3)
    val list = ring.findUnique("1".getBytes, 3)
    list should have size 1
    list should contain(MemberAddress("host1", 1))
  }

  it should "return unique set of nodes when we've more then replica count nodes in the ring" in {
    val members = (1 to 5).map { index => MemberAddress(s"host$index", index) }.toList
    val ring = ConsistentHashRing(members, 3)
    val list = ring.findUnique("1".getBytes, 3)
    list should have size 3
    list should contain(MemberAddress("host3", 3))
    list should contain(MemberAddress("host4", 4))
    list should contain(MemberAddress("host1", 1))
  }

  it should "return the right ringState that wraps around the HashRing" in {
    val ring = ConsistentHashRing(2)
    val host1 = MemberAddress("host1", 1)
    val host2 = MemberAddress("host2", 2)
    val host3 = MemberAddress("host3", 3)

    ring.sortedMap.put(10, VNode(host1, 1))
    ring.sortedMap.put(20, VNode(host2, 1))
    ring.sortedMap.put(30, VNode(host3, 1))
    ring.sortedMap.put(40, VNode(host3, 2))
    ring.sortedMap.put(50, VNode(host2, 2))
    ring.sortedMap.put(60, VNode(host1, 2))

    val ringState = ring.ringState
    ringState.ranges should have size(6)
    ringState.byNodes(host1) should contain(TokenRange(10, 19, VNode(host1, 1)))
    ringState.byNodes(host1) should contain(TokenRange(60, 9, VNode(host1, 2)))

    ringState.byNodes(host2) should contain(TokenRange(20, 29, VNode(host2, 1)))
    ringState.byNodes(host2) should contain(TokenRange(50, 59, VNode(host2, 2)))

    ringState.byNodes(host3) should contain(TokenRange(30, 39, VNode(host3, 1)))
    ringState.byNodes(host3) should contain(TokenRange(40, 49, VNode(host3, 2)))
  }
}
