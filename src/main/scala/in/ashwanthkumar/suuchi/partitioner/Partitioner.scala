package in.ashwanthkumar.suuchi.partitioner

import java.nio.ByteBuffer

trait Partitioner {
  def find(key: Array[Byte], replicaCount: Int): List[PartitionNode]
  def find(key: Array[Byte]) = find(key, 1)
}

class ConsistentHashPartitioner(hashRing: ConsistentHashRing) extends Partitioner {
  override def find(key: Array[Byte], replicaCount: Int): List[PartitionNode] = List(hashRing.find(key))
}

trait Hash {
  def hash[T](instance: T): Array[Byte]
}

class ConsistentHashRing {

  def find(key: Array[Byte]): PartitionNode = ???
}