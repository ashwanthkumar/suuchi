package in.ashwanthkumar.suuchi.cluster

import com.typesafe.config.Config

class TestStaticCluster(self: MemberAddress, config: Config, listeners: List[MemberListener])
    extends Cluster(config, listeners) {
  var members: Set[MemberAddress] = _

  override def start(seedProvider: SeedProvider): Cluster = {
    members = seedProvider.nodes.toSet
    this
  }
  override def nodes: scala.Iterable[MemberAddress] = members
  override def whoami: MemberAddress                = self
  override def stop(): Unit                         = {}

  def addNode(node: MemberAddress): Unit = {
    members ++= Set(node)
    this.onJoin.apply(node)
  }

  def removeNode(node: MemberAddress): Unit = {
    members --= Set(node)
    this.onLeave.apply(node)
  }
}

class TestStaticClusterProvider extends ClusterProvider {
  override def createCluster(self: MemberAddress,
                             config: Config,
                             listeners: scala.List[MemberListener]): Cluster = {
    new TestStaticCluster(self, config, listeners)
  }
  override def priority: Int = 1
}
