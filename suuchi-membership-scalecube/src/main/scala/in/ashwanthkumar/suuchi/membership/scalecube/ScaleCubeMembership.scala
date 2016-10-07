package in.ashwanthkumar.suuchi.membership.scalecube

import in.ashwanthkumar.suuchi.membership.{Bootstrapper, Member, Membership}
import io.scalecube.cluster.gossip.GossipConfig
import io.scalecube.cluster.membership.{MembershipEvent, MembershipConfig}
import io.scalecube.cluster.{Cluster, ClusterConfig, ICluster}
import io.scalecube.transport.{Address, TransportConfig}
import rx.functions.Action1

import scala.collection.JavaConversions._

class ScaleCubeMembership(port: Int) extends Membership {

  var cluster: ICluster = _

  override def bootstrap(bootstrapper: Bootstrapper): Membership = {
    val clusterConfig = ClusterConfig.builder()
      .transportConfig(
        TransportConfig.builder()
          .port(port).build()
      )
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
  override def nodes: Iterable[Member] = cluster.members().map(m => Member(m.id()))
  override def onJoin: (Member) => Unit = (m: Member) => println(s"$m has joined")
  override def start(): Membership = {
    cluster.listenMembership().subscribe(new MembershipEventListner(this))
    this
  }
  override def onLeave: (Member) => Unit = (m: Member) => println(s"$m has left")
  override def whoami: Member = Member(cluster.member().id())
}

class MembershipEventListner(membership: Membership) extends Action1[MembershipEvent] {
  override def call(t: MembershipEvent): Unit = t match {
    case event if event.isAdded => membership.onJoin.apply(Member(event.member().id()))
    case event if event.isRemoved => membership.onLeave.apply(Member(event.member().id()))
  }
}