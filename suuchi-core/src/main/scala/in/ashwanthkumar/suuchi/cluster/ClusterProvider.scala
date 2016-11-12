package in.ashwanthkumar.suuchi.cluster

import java.util.ServiceLoader


abstract class ClusterProvider {
  /**
   * Create Membership implementation using a [[MemberAddress]] for cluster communication and [[MemberListener]]
   *
   * @param self        [[MemberAddress]] of this node to use for cluster management
   * @param listeners   List of [[MemberListener]] to hook into as part of the returned
   *                    cluster implementation
   * @return            A Cluster implementation that's configured
   */
  def createCluster(self: MemberAddress, listeners: List[MemberListener]): Cluster

  /**
   * We use this method to sort when multiple providers are found. We'll pick the provider with highest value.
   * @return
   */
  def priority: Int
}

object ClusterProvider {
  def apply(clusterAddress: MemberAddress, listeners: List[MemberListener]) = {
    import scala.collection.JavaConversions._

    val providers = ServiceLoader.load(classOf[ClusterProvider])
      .iterator()
      .toList.sortBy(_.priority)

    providers.headOption
      .map(_.createCluster(clusterAddress, listeners))
      .getOrElse(
        throw new RuntimeException("No Cluster implementations found. Consider adding suuchi-cluster-atomix or suuchi-cluster-scalecube modules to your dependencies")
      )
  }
}
