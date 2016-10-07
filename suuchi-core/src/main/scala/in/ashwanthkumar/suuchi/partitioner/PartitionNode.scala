package in.ashwanthkumar.suuchi.partitioner

import in.ashwanthkumar.suuchi.membership.MemberAddress

case class VNode(node: MemberAddress, nodeReplicaId: Int) {
  def key = node.host+"_"+node.port+"_"+nodeReplicaId
}

case class TokenRange(start: Int, end: Int, node: VNode) {
  def range = start -> end
}

case class RingState(private[partitioner] val lastKnown: Int, ranges: List[TokenRange]) {
  def byNodes = ranges.groupBy(_.node.node)
}
