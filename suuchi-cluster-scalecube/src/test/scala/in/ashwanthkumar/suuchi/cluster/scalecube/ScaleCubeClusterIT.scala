package in.ashwanthkumar.suuchi.cluster.scalecube

import java.util.concurrent._

import com.typesafe.config.ConfigFactory
import in.ashwanthkumar.suuchi.cluster._
import org.scalatest.Matchers.{convertToAnyShouldWrapper, have}
import org.scalatest.{BeforeAndAfter, FlatSpec}

case class ExpectedMemberCount(joinLatch: Option[CountDownLatch] = None, removeLatch: Option[CountDownLatch] = None) extends MemberListener {
  /**
    * Triggered when a node represented by [[MemberAddress]] is added to the cluster
    */
  override def onJoin: (MemberAddress) => Unit = _ => joinLatch.foreach(_.countDown())
  /**
    * Triggered when a node represented by [[MemberAddress]] is removed from the cluster
    */
  override def onLeave: (MemberAddress) => Unit = _ => removeLatch.foreach(_.countDown())
}

class ScaleCubeClusterIT extends FlatSpec with BeforeAndAfter {
  val BASE_PORT = 20000
  var members: List[Cluster] = List()
  val latch = new CountDownLatch(4) // at least 4 members should have joined

  after {
    members.foreach(_.stop())
  }

  "ScaleCubeCluster" should "launch 5 nodes and say they have 5 nodes" in {
    val seedNode = new ScaleCubeCluster(configWithPort(BASE_PORT + 1),
      List(
        ExpectedMemberCount(joinLatch = Some(latch))
      )
    )
    members = List(seedNode.start(InMemorySeedProvider(List())))

    (2 to 5).foreach { i =>
      val bootstrapper = InMemorySeedProvider(List(seedNode.whoami))
      val member = ClusterProvider.apply(MemberAddress("localhost", BASE_PORT + i), configWithPort(BASE_PORT + i), Nil)
      members = members ++ List(member.start(bootstrapper))
    }
    latch.await(10, TimeUnit.SECONDS) // wait until all nodes have contacted with the seed node

    members.map(m => m.nodes).foreach(println)
    val totalNodes = members.head.nodes
    totalNodes should have size 5
  }

  def configWithPort(port: Int = BASE_PORT) =
    ConfigFactory.parseString(
      s"""
         |scalecube {
         | port = $port
         | gossip {
         |   interval = 200
         |   fanout = 5
         | }
         |}
    """.stripMargin)

}
