package in.ashwanthkumar.suuchi.partitioner

import java.nio.ByteBuffer
import java.util
import java.util.concurrent.ConcurrentMap

import scala.util.hashing.MurmurHash3

trait Partitioner {
  def find(key: Array[Byte], replicaCount: Int): List[VNode]
  def find(key: Array[Byte]) : List[VNode] = find(key, 1)
}

class ConsistentHashPartitioner(hashRing: ConsistentHashRing) extends Partitioner {
  override def find(key: Array[Byte], replicaCount: Int): List[VNode] = List(hashRing.find(key)).filter(_.isDefined).map(_.get)
}

trait Hash {
  def hash(bytes: Array[Byte]): Integer
}

object SuuchiHash extends Hash {
  override def hash(bytes: Array[Byte]): Integer = MurmurHash3.arrayHash(bytes)
}

class ConsistentHashRing(hashFn: Hash, vnodeFactor: Int = 3) {
  val sortedMap = new util.TreeMap[Integer, VNode]()

  def init(nodes: List[Node]): Unit = {
      nodes.foreach(add)
  }

  private def hash(vnode: VNode): Int = hashFn.hash(vnode.key.getBytes)

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
      val hashIdx = hashFn.hash(key)
      if(!sortedMap.containsKey(hashIdx)) {
        val newHashIdx = if(sortedMap.tailMap(hashIdx).isEmpty) sortedMap.firstKey() else sortedMap.tailMap(hashIdx).firstKey()
        Some(sortedMap.get(newHashIdx))
      } else {
        Some(sortedMap.get(hashIdx))
      }
    }
  }

  // USED ONLY FOR TESTS
  private[partitioner] def nodes = sortedMap.values()
}