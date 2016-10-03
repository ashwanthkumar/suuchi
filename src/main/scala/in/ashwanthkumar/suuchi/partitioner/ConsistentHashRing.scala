package in.ashwanthkumar.suuchi.partitioner

import java.util

import in.ashwanthkumar.suuchi.membership.MemberAddress

class ConsistentHashRing(hashFn: Hash, vnodeFactor: Int = 3) {
  val sortedMap = new util.TreeMap[Integer, VNode]()

  def init(nodes: List[MemberAddress]) = {
    nodes.foreach(add)
    this
  }

  private def hash(vnode: VNode): Int = hashFn.hash(vnode.key.getBytes)

  def add(node: MemberAddress) = {
    (1 to vnodeFactor).map(i => VNode(node, i)).foreach { vnode =>
      sortedMap.put(hash(vnode), vnode)
    }
    this
  }

  def remove(node: MemberAddress) = {
    (1 to vnodeFactor).map(i => VNode(node, i)).foreach { vnode =>
      sortedMap.remove(hash(vnode))
    }
    this
  }

  def find(key: Array[Byte]): Option[VNode] = {
    if (sortedMap.isEmpty) None
    else {
      val hashIdx = hashFn.hash(key)
      if (!sortedMap.containsKey(hashIdx)) {
        val newHashIdx = if (sortedMap.tailMap(hashIdx).isEmpty) sortedMap.firstKey() else sortedMap.tailMap(hashIdx).firstKey()
        Some(sortedMap.get(newHashIdx))
      } else {
        Some(sortedMap.get(hashIdx))
      }
    }
  }

  // USED ONLY FOR TESTS
  private[partitioner] def nodes = sortedMap.values()
}

object ConsistentHashRing {
  def apply(hashFn: Hash): ConsistentHashRing = new ConsistentHashRing(hashFn)

  def apply(): ConsistentHashRing = apply(SuuchiHash)

  def apply(nodes: List[MemberAddress]): ConsistentHashRing = apply(SuuchiHash).init(nodes)

  def apply(replication: Int): ConsistentHashRing = apply(SuuchiHash, replication)

  def apply(hashFn: Hash, replication: Int): ConsistentHashRing = new ConsistentHashRing(hashFn, replication)
}
