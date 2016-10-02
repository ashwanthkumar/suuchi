package in.ashwanthkumar.suuchi.partitioner

case class Node(id: Int, host: String)
case class VNode(node: Node, nodeReplicaId: Int) {
  def key = node.host+"_"+nodeReplicaId
}
