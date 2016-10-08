package in.ashwanthkumar.suuchi.membership.scalecube

import in.ashwanthkumar.suuchi.membership.{Bootstrapper, MemberAddress, Membership}
import io.scalecube.cluster.gossip.GossipConfig
import io.scalecube.cluster.membership.{MembershipConfig, MembershipEvent}
import io.scalecube.cluster.{Cluster, ClusterConfig, ICluster}
import io.scalecube.transport.{Address, TransportConfig}
import rx.functions.Action1

import scala.collection.JavaConversions._

class ScaleCubeMembership(port: Int, gossipConfig: Option[GossipConfig] = None) extends Membership {

  var cluster: ICluster = _

  override def bootstrap(bootstrapper: Bootstrapper): Membership = {
    val clusterConfig = ClusterConfig.builder()
      .transportConfig(
        TransportConfig.builder()
          .port(port).build()
      )
    gossipConfig.foreach(clusterConfig.gossipConfig)
    if (bootstrapper.nodes.isEmpty) {
      cluster = Cluster.joinAwait(clusterConfig.build())
    } else {
      val seedNodes = bootstrapper.nodes.map(m => Address.create(m.host, m.port))
      cluster = Cluster.joinAwait(
        clusterConfig
          .membershipConfig(MembershipConfig.builder.seedMembers(seedNodes).build)
          .gossipConfig(GossipConfig.builder().build())
          .build()
      )
    }

    this
  }
  override def stop(): Unit = cluster.shutdown().get()
  override def nodes: Iterable[MemberAddress] = cluster.members().map(m => MemberAddress(m.address().toString))
  override def onJoin: (MemberAddress) => Unit = (m: MemberAddress) => println(s"$m has joined")
  override def start(): Membership = {
    cluster.listenMembership().subscribe(new MembershipEventListener(this))
    this
  }
  override def onLeave: (MemberAddress) => Unit = (m: MemberAddress) => println(s"$m has left")
  override def whoami: MemberAddress = MemberAddress(cluster.member().address().toString)
}

class MembershipEventListener(membership: Membership) extends Action1[MembershipEvent] {
  override def call(t: MembershipEvent): Unit = t match {
    case event if event.isAdded =>
      membership.onJoin.apply(MemberAddress(event.member().address().toString))
    case event if event.isRemoved =>
      membership.onLeave.apply(MemberAddress(event.member().address().toString))
  }
}