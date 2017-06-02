package in.ashwanthkumar.suuchi.cluster.scalecube

import com.typesafe.config.ConfigFactory
import io.scalecube.cluster.gossip.GossipConfig
import org.scalatest.FlatSpec
import org.scalatest.Matchers.{be, convertToAnyShouldWrapper}

class ScaleCubeConfigTest extends FlatSpec {
  "ScalecubeConfig" should "parse the config correctly" in {
    val scaleCubeConfig =
      """
        |scalecube {
        |   port = 9000
        |}
      """.stripMargin

    val config = ScaleCubeConfig(ConfigFactory.parseString(scaleCubeConfig))
    config.port should be(9000)
    config.gossipConfig should be(None)
  }

  it should "parse the gossip settings as well from the config" in {
    val scaleCubeConfig =
      """
        |scalecube {
        |   port = 9000
        |   gossip {
        |     interval = 3000
        |     fanout = 5
        |   }
        |}
      """.stripMargin

    val config = ScaleCubeConfig(ConfigFactory.parseString(scaleCubeConfig))
    val expectedGossipConfig = GossipConfig.builder().gossipFanout(5).gossipInterval(3000).build()
    config.port should be(9000)
    config.gossipConfig.get.getGossipFanout should be(5)
    config.gossipConfig.get.getGossipInterval should be(3000)
  }

}
