package in.ashwanthkumar.suuchi.cluster

import java.util.ServiceLoader

import com.typesafe.config.Config

abstract class ClusterProvider {

  /**
   * Create Membership implementation using a [[MemberAddress]] for cluster communication and [[MemberListener]]
   *
   * @param self        [[MemberAddress]] of this node to use for cluster management
   * @param config      [[Config]] instance that's used for configuring the Cluster implementation. This also acts
   *                    as a way of passing additional information / configuration parameters required for the Cluster
   *                    implementation
   * @param listeners   List of [[MemberListener]] to hook into as part of the returned
   *                    cluster implementation
   * @return            A Cluster implementation that's configured
   */
  def createCluster(self: MemberAddress, config: Config, listeners: List[MemberListener]): Cluster

  /**
   * We use this method to sort when multiple providers are found. We'll pick the provider with highest value.
   * @return
   */
  def priority: Int
}

object ClusterProvider {
  def apply(self: MemberAddress, clusterConfig: Config, listeners: List[MemberListener]) = {
    import scala.collection.JavaConversions._

    val providers = ServiceLoader
      .load(classOf[ClusterProvider])
      .iterator()
      .toList
      .sortBy(_.priority)(Ordering[Int].reverse)

    providers.headOption
      .map(_.createCluster(self, clusterConfig, listeners))
      .getOrElse(
        throw new RuntimeException(
          "No Cluster implementations found. Consider adding suuchi-cluster-atomix or suuchi-cluster-scalecube modules to your dependencies")
      )
  }
}
