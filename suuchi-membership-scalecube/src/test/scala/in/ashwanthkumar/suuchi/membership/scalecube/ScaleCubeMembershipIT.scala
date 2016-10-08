package in.ashwanthkumar.suuchi.membership.scalecube

import java.util.concurrent.{CountDownLatch, TimeUnit}

import in.ashwanthkumar.suuchi.membership.{InMemoryBootstrapper, MemberAddress, Membership}
import io.scalecube.cluster.gossip.GossipConfig
import org.scalatest.Matchers.{be, convertToAnyShouldWrapper, have}
import org.scalatest.{BeforeAndAfter, FlatSpec}

class TestScaleCubeMembership(latch: CountDownLatch, port: Int, gossipConfig: Option[GossipConfig]) extends ScaleCubeMembership(port, gossipConfig) {
  override def onJoin: (MemberAddress) => Unit = (m: MemberAddress) => {
    super.onJoin(m)
    println("Counting down")
    latch.countDown()
  }
}

class ScaleCubeMembershipIT extends FlatSpec with BeforeAndAfter {
  val BASE_PORT = 20000
  var members: List[Membership] = List()
  val latch = new CountDownLatch(4) // at least 4 members should have joined

  before {
    val bootstrapper = InMemoryBootstrapper(List(MemberAddress("localhost", BASE_PORT + 1)))
    val gossipConfig = GossipConfig.builder().gossipInterval(5).build()
    val seedNode = new TestScaleCubeMembership(latch, BASE_PORT + 1, Some(gossipConfig))
    members = List(seedNode.bootstrap(InMemoryBootstrapper(List())))

    (2 to 5).foreach { i =>
      val member = new ScaleCubeMembership(BASE_PORT + i, Some(gossipConfig))
      members = members ++ List(member.bootstrap(bootstrapper))
    }
  }

  after {
    members.foreach(_.stop())
  }

  "ScaleCubeCluster" should "launch 5 nodes and say they have 5 nodes" in {
    members.foreach(_.start())
    val status = latch.await(60, TimeUnit.SECONDS) // wait until all nodes have contacted with the seed node
    status should be(true)

    members.map(m => m.nodes).foreach(println)
    val totalNodes = members.head.nodes
    totalNodes should have size 5
  }

}
