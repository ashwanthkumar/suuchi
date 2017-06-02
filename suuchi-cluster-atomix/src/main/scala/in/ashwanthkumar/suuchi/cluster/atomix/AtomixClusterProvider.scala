package in.ashwanthkumar.suuchi.cluster.atomix

import com.typesafe.config.Config
import in.ashwanthkumar.suuchi.cluster.{Cluster, ClusterProvider, MemberAddress, MemberListener}

/**
 * [[ClusterProvider]] wrapper for [[AtomixCluster]]. Please note the config passed to `createCluster`
 * should have a sub-configuration called "atomix". That'll be used for getting atomix specific settings.
 */
class AtomixClusterProvider extends ClusterProvider {

  /**
   * @inheritdoc
   */
  override def createCluster(self: MemberAddress,
                             config: Config,
                             listeners: List[MemberListener]) = {
    val atomixConfig = config.getConfig("atomix")
    new AtomixCluster(
      host = self.host,
      atomixPort = atomixConfig.getInt("port"),
      rpcPort = atomixConfig.getInt("rpc-port"),
      workDir = atomixConfig.getString("working-dir"),
      config = config,
      clusterIdentifier = atomixConfig.getString("cluster-id"),
      listeners = listeners
    )
  }

  /**
   * @inheritdoc
   */
  override def priority = 5
}
