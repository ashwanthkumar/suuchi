package in.ashwanthkumar.suuchi.membership

trait Bootstrapper {
  /**
   * Returns a list of host that're used to connect to form a cluster. These nodes always represent the
   * initial seed nodes. While different [[Membership]] implementations might have different guarantees,
   * It's generally a good practice to expose a single consistent copy of the nodes
   * as seed nodes.
   *
   * @return  List[MemberAddress] that represents seed nodes to connect to
   */
  def nodes: List[MemberAddress]
}

/**
 * Default in memory implementation of [[Bootstrapper]] to be used in tests and static
 * configuration file based environments.
 *
 * @param nodes List[MemberAddress] that represents seed nodes to connect to
 */
case class InMemoryBootstrapper(override val nodes: List[MemberAddress]) extends Bootstrapper
