package in.ashwanthkumar.suuchi.membership

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

/**
 * [[Cluster]] implementation takes care of forming a inter node system (aka) distributed,
 * which helps in ops like
 *
 * <ol>
 * <li>Helps identify if a node is up or down</li>
 * <li>Dynamically scale up and down your system</li>
 * <li>Supports custom Listeners who'll be notified when a node joins / exits the cluster</li>
 * </ol>
 *
 * @param listeners List[MemberListener] who would be notified about changes in cluster membership
 */
abstract class Cluster(listeners: List[MemberListener]) {
  /**
   * Start / Join a given cluster instance, given a [[SeedProvider]] instance
   * to identify the intial list of nodes. If you're starting a single node
   * cluster, consider using [[InMemorySeedProvider.EMPTY]].
   *
   * The List[MemberAddress] in SeedProvider.nodes represents the address of the cluster
   * communication and not that of the gRPC service. While you can technically still use the
   * existing gRPC as an underlying transport, but it would be a little hard for us to
   * find / build implementations that does it. Hence we assume the [[Cluster]] implementations
   * are free to choose any transport they deem fit for the type of cluster membership they provide.
   *
   * Anothe reason for making the assumption is systems like Apache Gossip (http://gossip.incubator.apache.org/)
   * uses UDP based transport while gRPC needs a reliable transport and hence uses TCP.
   *
   * @param seedProvider
   * @return
   */
  def start(seedProvider: SeedProvider): Cluster

  /**
   * Stop and release this cluster related resources. This could mean sending
   * a LEAVE message and/or stop underlying transport for sending and receiving
   * messages.
   */
  def stop(): Unit

  /**
   * Once this [[Cluster]] has been started using [[Cluster.start(SeedProvider)]] this method
   * should return list of all the nodes that are current part of this cluster.
   *
   * Depending on the type of [[Cluster]] implementation, it might take a while to converge
   * and report a correct number, but the application should not make any assumptions on that.
   * Any changes to the members in this [[Cluster]] will be notified to all [[MemberListener]]
   * objects.
   *
   * @return  List[MemberAddress] of all the nodes in this cluster.
   */
  def nodes: Iterable[MemberAddress]

  /**
   * Address of the current node when it is part of the cluster
   * @return
   */
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
