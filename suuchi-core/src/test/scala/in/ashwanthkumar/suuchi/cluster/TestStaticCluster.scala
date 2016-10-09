package in.ashwanthkumar.suuchi.cluster

class TestStaticCluster(self: MemberAddress, listeners: List[MemberListener]) extends Cluster(listeners) {
  var members: Set[MemberAddress] = _

  override def start(seedProvider: SeedProvider): Cluster = {
    members = seedProvider.nodes.toSet
    this
  }
  override def nodes: scala.Iterable[MemberAddress] = members
  override def whoami: MemberAddress = self
  override def stop(): Unit = {}

  def addNode(node: MemberAddress): Unit = {
    members ++= Set(node)
  }

  def removeNode(node: MemberAddress): Unit = {
    members --= Set(node)
  }
}

class TestStaticClusterProvider extends ClusterProvider {
  override def createCluster(self: MemberAddress, listeners: scala.List[MemberListener]): Cluster = {
    new TestStaticCluster(self, listeners)
  }
  override def priority: Int = 1
}
