package in.ashwanthkumar.suuchi.membership

trait SeedProvider {
  /**
   * Returns a list of host that're used to connect to form a cluster. These nodes always represent the
   * initial seed nodes. While different [[Cluster]] implementations might have different guarantees,
   * It's generally a good practice to expose a single consistent copy of the nodes
   * as seed nodes.
   *
   * @return  List[MemberAddress] that represents seed nodes to connect to
   */
  def nodes: List[MemberAddress]
}

/**
 * Default in memory implementation of [[SeedProvider]] to be used in tests and static
 * configuration file based environments.
 *
 * @param nodes List[MemberAddress] that represents seed nodes to connect to
 */
case class InMemorySeedProvider(override val nodes: List[MemberAddress]) extends SeedProvider

object InMemorySeedProvider {
  val EMPTY = InMemorySeedProvider(Nil)
}
