package in.ashwanthkumar.suuchi.partitioner

import java.util

import in.ashwanthkumar.suuchi.cluster.MemberAddress

case class Shard(id: Int, primaryPartition: VNode, followers: List[VNode]) {
  def key = id + "_" + primaryPartition.node.host + "_" + primaryPartition.node.port

  def getNodes = primaryPartition.node :: followers.map(_.node)
}

class CHRWithShards(hashFn: Hash, primaryPartitionsPerNode: Int, replicationFactor: Int = 2) {
  private val sortedMap = new util.TreeMap[Integer, Shard]()
  private val priority = new NodePriority

  def init(nodes: List[MemberAddress]) = {
    priority.init(nodes)
    nodes.foreach(node => add(node))
    this
  }

  private def hash(shard: Shard): Int = hashFn.hash(shard.key.getBytes)

  def followers(node: MemberAddress): List[VNode] = {
    priority.followers(replicationFactor - 1, node).map(address => VNode(address, 1, Follower))
  }

  private def add(node: MemberAddress) = {
    (1 to primaryPartitionsPerNode).map(i => Shard(i, VNode(node, 1, Primary), followers(node))).foreach { shard =>
      sortedMap.put(hash(shard), shard)
    }
    this
  }

  def findCandidate(hash: Integer): Shard = {
    if (sortedMap.containsKey(hash)) return sortedMap.get(hash)
    val tailMap = sortedMap.tailMap(hash)
    val newHash = if (tailMap.isEmpty) sortedMap.firstKey() else tailMap.firstKey()
    sortedMap.get(newHash)
  }

  def find(key: Array[Byte]): List[MemberAddress] = {
    if (sortedMap.isEmpty) return Nil

    val hashIdx = hashFn.hash(key)
    
    val candidates = findCandidate(hashIdx)

    candidates.getNodes
  }

  // USED ONLY FOR TESTS
  private[partitioner] def shards = sortedMap.values()

}

object CHRWithShards {
  def apply(hashFn: Hash, nodes: List[MemberAddress], primaryPartitionsPerNode: Int, replicationFactor: Int): CHRWithShards = new CHRWithShards(hashFn, primaryPartitionsPerNode, replicationFactor).init(nodes)
  def apply(nodes: List[MemberAddress], primaryPartitionsPerNode: Int, replicationFactor: Int): CHRWithShards = apply(SuuchiHash, nodes, primaryPartitionsPerNode, replicationFactor)
}
