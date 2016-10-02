package in.ashwanthkumar.suuchi.partitioner

import java.nio.ByteBuffer
import java.util
import java.util.concurrent.ConcurrentMap

trait Partitioner {
  def find(key: Array[Byte], replicaCount: Int): List[VNode]
  def find(key: Array[Byte]) = find(key, 1)
}

class ConsistentHashPartitioner(hashRing: ConsistentHashRing) extends Partitioner {
  override def find(key: Array[Byte], replicaCount: Int): List[VNode] = List(hashRing.find(key))
}

trait Hash {
  def hash[T](instance: T): Integer
}

class ConsistentHashRing(hash: Hash, vnodeFactor: Int = 3) {
  val sortedMap = new util.TreeMap[Integer, VNode]()

  def init(nodes: List[Node]): Unit = {
      nodes.foreach(add)
  }

  private def hash(vnode: VNode) = hash.hash(vnode.key)

  def add(node: Node) = {
    (1 to vnodeFactor).map(i => VNode(node, i)).foreach { vnode =>
      sortedMap.put(hash(vnode), vnode)
    }
  }

  def remove(node: Node) = {
    (1 to vnodeFactor).map(i => VNode(node, i)).foreach { vnode =>
      sortedMap.remove(hash(vnode))
    }
  }

  def find(key: Array[Byte]): Option[VNode] = {
    if (sortedMap.isEmpty) return None
    else {
      val hashIdx = hash.hash(key)
      if(!sortedMap.containsKey(hashIdx)) {
        val newHashIdx = if(sortedMap.tailMap(hashIdx).isEmpty) sortedMap.firstKey() else sortedMap.tailMap(hashIdx).firstKey()
        Some(sortedMap.get(newHashIdx))
      } else {
        Some(sortedMap.get(hashIdx))
      }
    }
  }
}