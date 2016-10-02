package in.ashwanthkumar.suuchi.partitioner

case class Node(id: Int, host: String)
case class PartitionNode(shard: Int, node: Node)
