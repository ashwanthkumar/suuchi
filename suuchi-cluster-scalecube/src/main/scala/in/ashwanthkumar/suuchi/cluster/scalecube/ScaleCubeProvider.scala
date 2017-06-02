package in.ashwanthkumar.suuchi.cluster.scalecube

import com.typesafe.config.Config
import in.ashwanthkumar.suuchi.cluster._

class ScaleCubeProvider extends ClusterProvider {
  /**
   * @inheritdoc
   */
  override def createCluster(self: MemberAddress, config: Config, listeners: List[MemberListener]): Cluster = {
    new ScaleCubeCluster(config, listeners = listeners)
  }
  /**
   * @inheritdoc
   */
  override def priority: Int = 4
}
