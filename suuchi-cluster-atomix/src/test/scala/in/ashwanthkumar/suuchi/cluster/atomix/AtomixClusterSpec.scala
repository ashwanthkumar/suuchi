package in.ashwanthkumar.suuchi.cluster.atomix

import java.nio.file.Files

import com.typesafe.config.ConfigFactory
import in.ashwanthkumar.suuchi.cluster.{Cluster, ClusterProvider, InMemorySeedProvider, MemberAddress}
import org.apache.commons.io.FileUtils
import org.scalatest.{BeforeAndAfter, FlatSpec}
import org.scalatest.Matchers.{convertToAnyShouldWrapper, have}

class AtomixClusterSpec extends FlatSpec with BeforeAndAfter {

  val BASE_PORT = 60000
  val raftDir = Files.createTempDirectory("suuchi-membership-it")

  var members: List[Cluster] = List()

  after {
    members.foreach(_.stop())
    FileUtils.deleteDirectory(raftDir.toFile)
  }

  def atomixConfig(port: Int) =
    ConfigFactory.parseString(
      s"""
         |atomix {
         |  port = $port # port used by atomix for cluster membership communication
         |  working-dir = "${raftDir.toString}" # location used for storing raft logs
         |  # cluster identifier to make sure all nodes are taking part in the right cluster.
         |  # You can also use environment specific identifiers to differentiate them.
         |  cluster-id = "suuchi-atomix-test-group"
         |  rpc-port = 8080 # port used for gRPC communication
         |}
    """.stripMargin)

  "Membership" should "launch 5 nodes and say they have 5 nodes" in {
    val bootstrapper = InMemorySeedProvider(List(MemberAddress("localhost", BASE_PORT + 1)))
    (1 to 5).foreach { i =>
      val memberPort = BASE_PORT + i
      //      val member = new AtomixCluster("localhost", memberPort, memberPort, raftDir.toString, "succhi-test-group", ConfigFactory.load())
      val member = ClusterProvider.apply(MemberAddress("localhost", memberPort), atomixConfig(memberPort), Nil)
      if (i > 1) {
        members = members ++ List(member.start(bootstrapper))
      } else {
        members = members ++ List(member.start(InMemorySeedProvider(List())))
      }
    }
    members.map(m => (m.nodes, m.whoami)).foreach(println)
    val totalNodes = members.head.nodes
    totalNodes should have size 5
  }

}
