package in.ashwanthkumar.suuchi.partitioner

import org.scalatest.FlatSpec
import org.scalatest.Matchers._

object IdentityHash extends Hash {
  override def hash(bytes: Array[Byte]): Integer = bytes.toString.toInt
}

class ConsistentHashRingSpec extends FlatSpec {
  "ConsistentHashRing" should "pin nodes into the ring accounting for virtual nodes" in {
    val ring = new ConsistentHashRing(SuuchiHash, 3)
    ring.init(List(Node(1, "host1"), Node(2, "host2"), Node(3, "host3")))

    ring.nodes.size() should be(9)

    ring.add(Node(100, "host100"))
    ring.nodes.size() should be(12)
  }

  it should "remove nodes & its replica nodes on remove" in {
    val ring = new ConsistentHashRing(SuuchiHash, 3)
    ring.init(List(Node(1, "host1"), Node(2, "host2"), Node(3, "host3")))

    ring.remove(Node(1, "host1"))
    ring.nodes.size should be(6)

    ring.remove(Node(2, "host2"))
    ring.nodes.size should be(3)

    ring.remove(Node(3, "host3"))
    ring.nodes.size should be(0)

    ring.remove(Node(4, "host4"))
    ring.nodes.size should be(0)
  }
}
