package in.ashwanthkumar.suuchi.rpc

import in.ashwanthkumar.suuchi.cluster.MemberAddress
import in.ashwanthkumar.suuchi.examples.rpc.generated._
import in.ashwanthkumar.suuchi.partitioner.ConsistentHashRing

import scala.concurrent.Future

class SuuchiShardService(ring: ConsistentHashRing, replicationFactor: Int)
  extends ShardsGrpc.Shards {
  private[rpc] def toNode(m: MemberAddress): Node = {
    Node(host = m.host, port = m.port)
  }

  override def info(request: ShardInfoRequest) = Future.successful {
    val shards = ring.ringState.withReplication(replicationFactor).map {
      case (token, replica) =>
        Shard(start = token.start, end = token.end, nodes = replica.map(m => toNode(m)))
    }.toSeq
    ShardInfoResponse(shards)
  }
}
