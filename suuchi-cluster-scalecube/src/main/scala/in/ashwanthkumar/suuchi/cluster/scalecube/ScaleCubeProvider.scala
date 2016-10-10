package in.ashwanthkumar.suuchi.cluster.scalecube

import in.ashwanthkumar.suuchi.cluster._

class ScaleCubeProvider extends ClusterProvider {
  /**
   * @inheritdoc
   */
  override def createCluster(self: MemberAddress, listeners: List[MemberListener]): Cluster = {
    new ScaleCubeCluster(self.port, listeners = listeners)
  }
  /**
   * @inheritdoc
   */
  override def priority: Int = 4
}
