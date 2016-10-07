package in.ashwanthkumar.suuchi.membership.scalecube

import in.ashwanthkumar.suuchi.membership.{InMemoryBootstrapper, MemberAddress, Membership}
import org.scalatest.Matchers.{convertToAnyShouldWrapper, have}
import org.scalatest.{BeforeAndAfter, FlatSpec}

class ScaleCubeMembershipIT extends FlatSpec with BeforeAndAfter {
  val BASE_PORT = 20000
  var members: List[Membership] = List()

  before {
    val bootstrapper = InMemoryBootstrapper(List(MemberAddress("localhost", BASE_PORT + 1)))
    (1 to 5).foreach { i =>
      val memberPort = BASE_PORT + i
      val member = new ScaleCubeMembership(memberPort)
      if (i > 1) {
        members = members ++ List(member.bootstrap(bootstrapper))
      } else {
        members = members ++ List(member.bootstrap(InMemoryBootstrapper(List())))
      }
    }
  }

  after {
    members.foreach(_.stop())
  }

  "ScaleCubeCluster" should "launch 5 nodes and say they have 5 nodes" in {
    members.foreach(_.start())
    Thread.sleep(3 * 1000) // wait for all the nodes to come up and gossip

    members.map(m => m.nodes).foreach(println)
    val totalNodes = members.head.nodes
    totalNodes should have size 5
  }

}
