package in.ashwanthkumar.suuchi.partitioner

import java.util

import in.ashwanthkumar.suuchi.cluster.MemberAddress

case class Shard(primaryPartition: VNode, follower: List[VNode]) {
  def key = primaryPartition.node.host + "_" + primaryPartition.node.port + "_" + primaryPartition.nodeReplicaId

  def getNodes = primaryPartition.node :: follower.map(_.node)
}

class CHRWithShards(hashFn: Hash, partitionsPerNode: Int, replicationFactor: Int = 2) {
  val sortedMap = new util.TreeMap[Integer, Shard]()

  def init(nodes: List[MemberAddress]) = {
    nodes.foreach(node => add(node, nodes))
    this
  }

  private def hash(shard: Shard): Int = hashFn.hash(shard.key.getBytes)

  def followers(nodes: List[MemberAddress]): List[VNode] = {
    nodes.flatMap(node => (1 until replicationFactor).map(i => VNode(node, i, Follower)))
  }

  def add(node: MemberAddress, nodes: List[MemberAddress]) = {
    // FIXME: followers based on nodes.filterNot will lead to a skew in first elements in the list
    (1 to partitionsPerNode).map(i => Shard(VNode(node, 1, Primary), followers(nodes.filterNot(_.equals(node))))).foreach { shard =>
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

}
