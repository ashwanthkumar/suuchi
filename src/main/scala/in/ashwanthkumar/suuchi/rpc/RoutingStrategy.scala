package in.ashwanthkumar.suuchi.rpc

import in.ashwanthkumar.suuchi.membership.MemberAddress
import in.ashwanthkumar.suuchi.partitioner.{ConsistentHashPartitioner}

trait RoutingStrategy {
  /**
   * Decides if the incoming message should be forwarded or handled by the current node itself.
   *
   * @param incomingMessage Actual message that was sent as part of the unary method
   * @tparam ReqT Type of the input Message
   * @return  Some(MemberAddress) - if the request is meant to be forwarded
   *          <p> None - if the request can be handled by the current node itself
   */
  def route[ReqT](incomingMessage: ReqT): Option[MemberAddress]
}

/**
 * Consistently forward the requests to a node - useful in tests or while debugging
 * @param memberAddress
 */
class AlwaysRouteTo(memberAddress: MemberAddress) extends RoutingStrategy {
  /**
   * @inheritdoc
   */
  override def route[ReqT](incomingMessage: ReqT): Option[MemberAddress] = Some(memberAddress)
}


class ConsistentHashingRouter(partitioner: ConsistentHashPartitioner) extends RoutingStrategy {
  /**
    * Uses a ConsistentHash based Partitioner to find the right node for the incoming message.
    *
    * @param incomingMessage Actual message that was sent as part of the unary method
    * @tparam ReqT Type of the input Message
    * @return Some(MemberAddress) - if the request is meant to be forwarded
    *         <p> None - if the request can be handled by the current node itself
    */
  override def route[ReqT](incomingMessage: ReqT): Option[MemberAddress] = {
    // FIXME: message translation + underlying partitioner contract needs some minor fix.
    partitioner.find(???).map(vnode => MemberAddress(vnode.node.host, 0)).headOption
  }
}