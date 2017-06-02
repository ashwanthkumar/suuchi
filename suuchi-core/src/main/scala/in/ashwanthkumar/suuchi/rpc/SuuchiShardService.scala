package in.ashwanthkumar.suuchi.rpc

import in.ashwanthkumar.suuchi.cluster.MemberAddress
import in.ashwanthkumar.suuchi.partitioner.ConsistentHashRing
import in.ashwanthkumar.suuchi.rpc.generated.SuuchiRPC.{
  Node,
  Shard,
  ShardInfoRequest,
  ShardInfoResponse
}
import in.ashwanthkumar.suuchi.rpc.generated.{ShardsGrpc, SuuchiRPC}
import io.grpc.stub.StreamObserver

import scala.collection.JavaConverters._

class SuuchiShardService(ring: ConsistentHashRing, replicationFactor: Int)
    extends ShardsGrpc.ShardsImplBase {
  override def info(request: ShardInfoRequest,
                    responseObserver: StreamObserver[ShardInfoResponse]): Unit = {
    val infoBuilder = ShardInfoResponse.newBuilder()
    ring.ringState.withReplication(replicationFactor).foreach {
      case (token, replica) =>
        val shard = Shard
          .newBuilder()
          .setStart(token.start)
          .setEnd(token.end)
          .addAllNodes(replica.map(m => toNode(m)).asJava)
          .build()

        infoBuilder.addShard(shard)
    }

    responseObserver.onNext(infoBuilder.build())
    responseObserver.onCompleted()
  }

  private[rpc] def toNode(m: MemberAddress): SuuchiRPC.Node = {
    Node.newBuilder().setHost(m.host).setPort(m.port).build()
  }
}
