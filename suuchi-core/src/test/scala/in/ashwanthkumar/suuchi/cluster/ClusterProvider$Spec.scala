package in.ashwanthkumar.suuchi.cluster

import org.scalatest.FlatSpec
import org.scalatest.Matchers.{convertToAnyShouldWrapper, be, have}

class ClusterProvider$Spec extends FlatSpec {
  "ClusterProvider" should "return TestStaticCluster instance by doing service loading" in {
    val cluster = ClusterProvider(MemberAddress("host1", 1), Nil)
    cluster.start(InMemorySeedProvider(List(MemberAddress("host1", 1), MemberAddress("host2", 2))))

    cluster.nodes should have size 2
    cluster.whoami should be(MemberAddress("host1", 1))
  }
}
