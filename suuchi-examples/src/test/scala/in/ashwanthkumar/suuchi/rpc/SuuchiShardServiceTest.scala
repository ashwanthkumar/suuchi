package in.ashwanthkumar.suuchi.rpc

import in.ashwanthkumar.suuchi.cluster.MemberAddress
import in.ashwanthkumar.suuchi.examples.rpc.generated.SuuchiRPC.{ShardInfoRequest, ShardInfoResponse}
import in.ashwanthkumar.suuchi.partitioner.ConsistentHashRing
import io.grpc.stub.StreamObserver
import org.mockito.ArgumentCaptor
import org.mockito.Mockito._
import org.scalatest.FlatSpec
import org.scalatest.Matchers.{be, convertToAnyShouldWrapper}

class SuuchiShardServiceTest extends FlatSpec {
  "SuuchiShardService" should "return shardInfo details for a given CHRing" in {
    val nodes =
      List(MemberAddress("host1", 1), MemberAddress("host2", 2), MemberAddress("host3", 3))
    val ring = ConsistentHashRing(nodes, partitionsPerNode = 3)

    val observer = mock(classOf[StreamObserver[ShardInfoResponse]])

    val service: SuuchiShardService = new SuuchiShardService(ring, replicationFactor = 2)
    service.info(ShardInfoRequest.newBuilder().build(), observer)

    val responseCaptor = ArgumentCaptor.forClass(classOf[ShardInfoResponse])
    verify(observer, times(1)).onNext(responseCaptor.capture())
    verify(observer, times(1)).onCompleted()

    val response: ShardInfoResponse = responseCaptor.getValue
    response.getShardCount should be(9) // 3 nodes * 3 partitions per node
  }
}
