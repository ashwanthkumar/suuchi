package in.ashwanthkumar.suuchi.partitioner

import java.nio.ByteBuffer
import java.util
import java.util.concurrent.ConcurrentMap

import in.ashwanthkumar.suuchi.membership.MemberAddress

import scala.util.hashing.MurmurHash3

trait Partitioner {
  def find(key: Array[Byte], replicaCount: Int): List[MemberAddress]
  def find(key: Array[Byte]) : List[MemberAddress] = find(key, 1)
}

class ConsistentHashPartitioner(hashRing: ConsistentHashRing) extends Partitioner {
  override def find(key: Array[Byte], replicaCount: Int): List[MemberAddress] = {
    hashRing.findNUnique(key, replicaCount)
  }
}
object ConsistentHashPartitioner {
  def apply(nodes: List[MemberAddress]) = new ConsistentHashPartitioner(ConsistentHashRing(nodes))
  def apply(ring: ConsistentHashRing) = new ConsistentHashPartitioner(ring)
}

trait Hash {
  def hash(bytes: Array[Byte]): Integer
}

object SuuchiHash extends Hash {
  override def hash(bytes: Array[Byte]): Integer = MurmurHash3.arrayHash(bytes)
}
