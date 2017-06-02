package in.ashwanthkumar.suuchi.cluster

import com.typesafe.config.Config
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.FlatSpec
import org.scalatest.Matchers.{be, convertToAnyShouldWrapper, have}
import org.scalatest.mockito.MockitoSugar._

class ClusterProviderSpec extends FlatSpec {
  "ClusterProvider" should "return TestStaticCluster instance by doing service loading" in {
    val emptyFn: (MemberAddress) => Unit = (m: MemberAddress) => {}
    val listener                         = mock[MemberListener]
    when(listener.onJoin).thenReturn(emptyFn)
    when(listener.onLeave).thenReturn(emptyFn)

    val config  = mock[Config]
    val cluster = ClusterProvider(MemberAddress("host1", 1), config, List(listener))
    cluster.start(InMemorySeedProvider(List(MemberAddress("host1", 1), MemberAddress("host2", 2))))

    cluster.nodes should have size 2
    cluster.whoami should be(MemberAddress("host1", 1))

    cluster.asInstanceOf[TestStaticCluster].addNode(MemberAddress("host3", 3))
    cluster.nodes should have size 3
    verify(listener, times(1)).onJoin

    cluster.asInstanceOf[TestStaticCluster].removeNode(MemberAddress("host3", 3))
    cluster.nodes should have size 2
    verify(listener, times(1)).onLeave
  }
}
