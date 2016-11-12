package in.ashwanthkumar.suuchi.router

import com.google.protobuf.ByteString
import in.ashwanthkumar.suuchi.cluster.MemberAddress
import in.ashwanthkumar.suuchi.partitioner.{ConsistentHashRing, ConsistentHashPartitioner}
import org.slf4j.LoggerFactory

import scala.language.reflectiveCalls

trait RoutingStrategy {
  /**
   * Decides if the incoming message should be forwarded or handled by the current node itself.
   *
   * @tparam ReqT Type of the input Message
   * @return  List[MemberAddress]
   *          <p> > 1 when there are multiple nodes to which this needs to be replicated to including the current node
   *          <p> = 1 when replication factor is 1
   *          <p> Nil - should never happen unless there's a bug
   */
  def route[ReqT]: PartialFunction[ReqT, List[MemberAddress]]
}
object RoutingStrategy {
  type WithKey = {def getKey: ByteString}
}

/**
 * Always forward the requests to a given node - useful in tests or while debugging
 * @param members
 */
class AlwaysRouteTo(members: MemberAddress*) extends RoutingStrategy {
  private val log = LoggerFactory.getLogger(getClass)
  /**
   * @inheritdoc
   */
  override def route[ReqT]: PartialFunction[ReqT, List[MemberAddress]] = {
    case msg: RoutingStrategy.WithKey =>
      members.toList
  }
}

/**
 * Uses a ConsistentHash based Partitioner to find the right node for the incoming message.
 * @param partitioner - which is an implementation of ConsistentHashPartitioner
 **/
class ConsistentHashingRouting(partitioner: ConsistentHashPartitioner, nrReplicas: Int) extends RoutingStrategy {
  override def route[ReqT]: PartialFunction[ReqT, List[MemberAddress]] = {
    case msg: RoutingStrategy.WithKey => partitioner.find(msg.getKey.toByteArray, nrReplicas)
  }
}

object ConsistentHashingRouting {
  def apply(nrReplicas: Int, partitionsPerNode: Int, nodes: MemberAddress*) = {
    new ConsistentHashingRouting(ConsistentHashPartitioner(nodes.toList, partitionsPerNode), nrReplicas)
  }
}
