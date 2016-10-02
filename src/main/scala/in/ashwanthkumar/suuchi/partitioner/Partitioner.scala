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
  override def find(key: Array[Byte], replicaCount: Int): List[VNode] = {
    // FIXME: Doesn't take into account key's replica information
    // We will come to that when we do replication
    List(hashRing.find(key))
      .filter(_.isDefined)
      .take(replicaCount)
      .map(_.get)
  }
}
object ConsistentHashPartitioner {
  def apply() = new ConsistentHashPartitioner(new ConsistentHashRing(SuuchiHash))
}

trait Hash {
  def hash(bytes: Array[Byte]): Integer
}

object SuuchiHash extends Hash {
  override def hash(bytes: Array[Byte]): Integer = MurmurHash3.arrayHash(bytes)
}

