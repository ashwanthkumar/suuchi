package in.ashwanthkumar.suuchi.partitioner

import in.ashwanthkumar.suuchi.membership.MemberAddress

case class VNode(node: MemberAddress, nodeReplicaId: Int) {
  def key = node.host+"_"+node.port+"_"+nodeReplicaId
}
