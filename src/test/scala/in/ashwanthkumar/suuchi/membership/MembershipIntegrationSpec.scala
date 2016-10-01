package in.ashwanthkumar.suuchi.membership

import java.nio.file.Files

import org.apache.commons.io.FileUtils
import org.scalatest.{BeforeAndAfter, FlatSpec}
import org.scalatest.Matchers.{convertToAnyShouldWrapper, be, have, size}

class MembershipIntegrationSpec extends FlatSpec with BeforeAndAfter {

  val BASE_PORT = 60000
  val raftDir = Files.createTempDirectory("suuchi-membership-it")

  var members: List[AtomixMembership] = List()

  before {
    val bootstrapper = InMemoryBootstrapper(List(MemberAddress("localhost", BASE_PORT + 1)))
    (1 to 5).foreach { i =>
      val memberPort = BASE_PORT + i
      val member = new AtomixMembership("localhost", memberPort, raftDir.toString, "succhi-test-group")
      if (i > 1) {
        members = members ++ List(member.bootstrap(bootstrapper))
      } else {
        members = members ++ List(member.bootstrap(InMemoryBootstrapper(List())))
      }
    }
  }

  after {
    members.foreach(_.stop())
    FileUtils.deleteDirectory(raftDir.toFile)
  }

  "Membership" should "launch 5 nodes and say they have 5 nodes" in {
    members.foreach(_.start())
//    Thread.sleep(10 * 1000) // wait for all the nodes to come up

    members.map(m => (m.nodes, m.me)).foreach(println)
    val totalNodes = members.head.nodes
    totalNodes should have size 5
  }
}
