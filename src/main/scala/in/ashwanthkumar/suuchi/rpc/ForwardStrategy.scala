package in.ashwanthkumar.suuchi.rpc

import in.ashwanthkumar.suuchi.membership.MemberAddress

trait ForwardStrategy {
  /**
   * Decides if the incoming message should be forwarded or handled by the current node itself.
   *
   * @param incomingMessage Actual message that was sent as part of the unary method
   * @tparam ReqT Type of the input Message
   * @return  Some(MemberAddress) - if the request is meant to be forwarded
   *          <p> None - if the request can be handled by the current node itself
   */
  def shouldForward[ReqT](incomingMessage: ReqT): Option[MemberAddress]
}

/**
 * Consistently forward the requests to a node - useful in tests or while debugging
 * @param memberAddress
 */
class AlwaysForwardTo(memberAddress: MemberAddress) extends ForwardStrategy {
  /**
   * @inheritdoc
   */
  override def shouldForward[ReqT](incomingMessage: ReqT): Option[MemberAddress] = Some(memberAddress)
}
