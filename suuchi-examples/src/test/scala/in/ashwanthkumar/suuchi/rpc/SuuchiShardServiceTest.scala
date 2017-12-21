package in.ashwanthkumar.suuchi.rpc

import in.ashwanthkumar.suuchi.cluster.MemberAddress
import in.ashwanthkumar.suuchi.examples.rpc.generated.{ShardInfoRequest, ShardInfoResponse}
import in.ashwanthkumar.suuchi.partitioner.ConsistentHashRing
import io.grpc.stub.StreamObserver
import org.mockito.Mockito._
import org.scalatest.FlatSpec
import org.scalatest.Matchers.{be, convertToAnyShouldWrapper}
import org.scalatest.concurrent.ScalaFutures.whenReady

class SuuchiShardServiceTest extends FlatSpec {
  "SuuchiShardService" should "return shardInfo details for a given CHRing" in {
    val nodes =
      List(MemberAddress("host1", 1), MemberAddress("host2", 2), MemberAddress("host3", 3))
    val ring = ConsistentHashRing(nodes, partitionsPerNode = 3)

    val observer = mock(classOf[StreamObserver[ShardInfoResponse]])

    val service: SuuchiShardService = new SuuchiShardService(ring, replicationFactor = 2)
    whenReady(service.info(ShardInfoRequest())) { response =>
      response.shard.size should be(9) // 3 nodes * 3 partitions per node
    }
  }
}
