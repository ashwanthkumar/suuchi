package in.ashwanthkumar.suuchi.membership

import java.util.ServiceLoader

/**
 * Listeners are notified whenever there are new nodes joining the cluster
 * or existing nodes removed from the cluster
 */
trait MemberListener {
  /**
   * Triggered when a node represented by [[MemberAddress]] is added to the cluster
   */
  def onJoin: MemberAddress => Unit
  /**
   * Triggered when a node represented by [[MemberAddress]] is removed from the cluster
   */
  def onLeave: MemberAddress => Unit
}

abstract class Membership(listeners: List[MemberListener]) {
  def start(seedProvider: SeedProvider): Membership

  def stop(): Unit

  def nodes: Iterable[MemberAddress]

  def whoami: MemberAddress

  /**
   * Handler for Membership implementations to bind when new members join the cluster.
   */
  final def onJoin: MemberAddress => Unit = m => listeners.foreach(_.onJoin(m))

  /**
   * Handler for Membership implementations to bind when new members leave the cluster
   * or a FailureDetector implementation has detected the node as un-reachable. Either
   * ways the node can't participate in regular activity of the cluster and has to be
   * removed from it's duty.
   */
  final def onLeave: MemberAddress => Unit = m => listeners.foreach(_.onLeave(m))
}
