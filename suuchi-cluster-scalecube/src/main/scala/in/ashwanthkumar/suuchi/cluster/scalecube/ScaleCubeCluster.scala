package in.ashwanthkumar.suuchi.cluster.scalecube

import com.typesafe.config.Config
import in.ashwanthkumar.suuchi.cluster.{MemberAddress, MemberListener, SeedProvider, Cluster => SuuchiCluster}
import io.scalecube.cluster.gossip.GossipConfig
import io.scalecube.cluster.membership.{MembershipConfig, MembershipEvent}
import io.scalecube.cluster.{Cluster, ClusterConfig, ICluster}
import io.scalecube.transport.{Address, TransportConfig}
import org.slf4j.LoggerFactory
import rx.lang.scala.ImplicitFunctionConversions._

import scala.collection.JavaConversions._
import scala.language.implicitConversions

case class ScaleCubeConfig(port: Int, gossipConfig: Option[GossipConfig])
object ScaleCubeConfig {
  def apply(config: Config): ScaleCubeConfig = {
    val scalecube = config.getConfig("scalecube")
    ScaleCubeConfig(
      port = scalecube.getInt("port"),
      gossipConfig = toGossipConfig(scalecube)
    )
  }

  private[this] def toGossipConfig(scalecube: Config): Option[GossipConfig] = {
    if(scalecube.hasPath("gossip")) {
      val gConfig = scalecube.getConfig("gossip")
      val gossipConfigBuilder = GossipConfig.builder()
      if(gConfig.hasPath("interval"))  {
        gossipConfigBuilder.gossipInterval(gConfig.getInt("interval"))
      }
      if(gConfig.hasPath("fanout")) {
        gossipConfigBuilder.gossipFanout(gConfig.getInt("fanout"))
      }
      Some(gossipConfigBuilder.build())
    } else None
  }

}

class ScaleCubeCluster(clusterConfig: Config, listeners: List[MemberListener]) extends SuuchiCluster(clusterConfig, listeners) {
  protected val log = LoggerFactory.getLogger(getClass)
  lazy val config = ScaleCubeConfig.apply(clusterConfig)

  var cluster: ICluster = _

  override def start(seedProvider: SeedProvider): SuuchiCluster = {
    val clusterConfig = ClusterConfig.builder()
      .transportConfig(
        TransportConfig.builder()
          .port(config.port).build()
      )
    config.gossipConfig.foreach(clusterConfig.gossipConfig)
    if (seedProvider.nodes.isEmpty) {
      cluster = Cluster.joinAwait(clusterConfig.build())
    } else {
      val seedNodes = seedProvider.nodes.map(m => Address.create(m.host, m.port))
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
  override def whoami: MemberAddress = MemberAddress(cluster.address().toString)
}
