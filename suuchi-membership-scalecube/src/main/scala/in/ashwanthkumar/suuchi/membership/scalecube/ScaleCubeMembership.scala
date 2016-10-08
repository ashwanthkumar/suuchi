package in.ashwanthkumar.suuchi.membership.scalecube

import in.ashwanthkumar.suuchi.membership.{Bootstrapper, MemberAddress, Membership}
import io.scalecube.cluster.gossip.GossipConfig
import io.scalecube.cluster.membership.{MembershipConfig, MembershipEvent}
import io.scalecube.cluster.{Cluster, ClusterConfig, ICluster}
import io.scalecube.transport.{Address, TransportConfig}
import org.slf4j.LoggerFactory
import rx.lang.scala.ImplicitFunctionConversions._

import scala.collection.JavaConversions._
import scala.language.implicitConversions

class ScaleCubeMembership(port: Int, gossipConfig: Option[GossipConfig] = None) extends Membership {
  protected val log = LoggerFactory.getLogger(getClass)

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
          .build()
      )
    }
    cluster.listenMembership()
      .filter({ m: MembershipEvent => m.isAdded })
      .map[MemberAddress]({ m: MembershipEvent => MemberAddress(m.member().address().toString) })
      .subscribe(this.onJoin)

    cluster.listenMembership()
      .filter({ m: MembershipEvent => m.isRemoved })
      .map[MemberAddress]({ m: MembershipEvent => MemberAddress(m.member().address().toString) })
      .subscribe(this.onLeave)
    this
  }
  override def stop(): Unit = cluster.shutdown().get()
  override def nodes: Iterable[MemberAddress] = cluster.members().map(m => MemberAddress(m.address().toString))
  override def start(): Membership = {
    /* We would have started the member and atempted to have joined the cluster in bootstrap itself */
    this
  }
  override def onJoin: (MemberAddress) => Unit = (m: MemberAddress) => log.info(s"[$whoami] $m has joined")
  override def onLeave: (MemberAddress) => Unit = (m: MemberAddress) => log.info(s"[$whoami] $m has left")
  override def whoami: MemberAddress = MemberAddress(cluster.address().toString)
}
