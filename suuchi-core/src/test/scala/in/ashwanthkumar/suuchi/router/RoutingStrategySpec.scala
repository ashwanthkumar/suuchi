package in.ashwanthkumar.suuchi.router

import com.google.protobuf.ByteString
import in.ashwanthkumar.suuchi.membership.MemberAddress
import org.scalatest.FlatSpec
import org.scalatest.Matchers._

case class IntReq(i: Int) {
  def getKey: ByteString = ByteString.copyFrom(i.toString.getBytes)
}
class RoutingStrategySpec extends FlatSpec {
  "ConsistentHashingRoutingStrategy" should "route incoming requests WithKey to appropriate nodes" in {
    val routingStrategy = ConsistentHashingRouting(2, List(MemberAddress("host1:1"),MemberAddress("host2:2"),MemberAddress("host3:3")):_*)
    routingStrategy.route(IntReq(100)).size should be(2)
    routingStrategy.route(IntReq(100)).distinct.size should be(2)
  }
}
