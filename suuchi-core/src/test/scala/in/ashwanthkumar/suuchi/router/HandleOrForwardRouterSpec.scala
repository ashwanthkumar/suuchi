package in.ashwanthkumar.suuchi.router

import in.ashwanthkumar.suuchi.cluster.MemberAddress
import io.grpc.ServerCall.Listener
import io.grpc._
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.FlatSpec

class NeverRoute extends RoutingStrategy {
  /**
   * @inheritdoc
   */
  override def route[ReqT]: PartialFunction[ReqT, List[MemberAddress]] = PartialFunction.empty
}

class NoAliveNodes extends RoutingStrategy {
  /**
   * @inheritdoc
   */
  override def route[ReqT]: PartialFunction[ReqT, List[MemberAddress]] = {
    case _ => Nil
  }
}

class HandleOrForwardRouterSpec extends FlatSpec {
  "Router" should "not forward messages if routing strategy doesn't say so" in {
    val router = new HandleOrForwardRouter(new NeverRoute(), MemberAddress("host2", 1))
    verifyInteractions(router, isForwarded = false, isHandledLocally = true)
  }

  it should "not forward messages if no nodes are alive" in {
    val router = new HandleOrForwardRouter(new NoAliveNodes(), MemberAddress("host2", 1))
    verifyInteractions(router, isForwarded = false, isHandledLocally = false)
  }

  it should "not forward message when router emits node to self" in {
    val router = new HandleOrForwardRouter(new AlwaysRouteTo(MemberAddress("host2", 1)), MemberAddress("host2", 1))
    verifyInteractions(router, isForwarded = false, isHandledLocally = true)
  }

  it should "forward message when router says so" in {
    val router = new HandleOrForwardRouter(new AlwaysRouteTo(MemberAddress("host1", 1)), MemberAddress("host2", 1)) {
      // mocking the actual forward implementation
      override def forward[RespT, ReqT](method: MethodDescriptor[ReqT, RespT], headers: Metadata, incomingRequest: ReqT, destination: MemberAddress): RespT = 1.asInstanceOf[RespT]
    }
    verifyInteractions(router, isForwarded = true, isHandledLocally = false)
  }

  it should "forward message to next node in the ring if the first one fails" in {
    val router = new HandleOrForwardRouter(new AlwaysRouteTo(MemberAddress("host1", 1), MemberAddress("host2", 2)), MemberAddress("host2", 1)) {
      var shouldFail = true
      // mocking the actual forward implementation
      override def forward[RespT, ReqT](method: MethodDescriptor[ReqT, RespT], headers: Metadata, incomingRequest: ReqT, destination: MemberAddress): RespT = {
        if (shouldFail) {
          shouldFail = false
          throw new RuntimeException("An exception happened while trying to forward the request")
        } else {
          1.asInstanceOf[RespT]
        }
      }
    }
    verifyInteractions(router, isForwarded = true, isHandledLocally = false)
  }

  it should "not forward message to any node in the ring if all node forwards fail" in {
    val router = new HandleOrForwardRouter(new AlwaysRouteTo(MemberAddress("host1", 1), MemberAddress("host2", 2)), MemberAddress("host2", 1)) {
      // mocking the actual forward implementation
      override def forward[RespT, ReqT](method: MethodDescriptor[ReqT, RespT], headers: Metadata, incomingRequest: ReqT, destination: MemberAddress): RespT = {
        throw new RuntimeException("An exception happened while trying to forward the request")
      }
    }
    verifyInteractions(router, isForwarded = false, isHandledLocally = false)
  }

  def verifyInteractions(router: HandleOrForwardRouter, isForwarded: Boolean, isHandledLocally: Boolean): Unit = {
    val serverCall = mock(classOf[ServerCall[Int, Int]])
    val serverMethodDesc = mock(classOf[MethodDescriptor[Int, Int]])
    when(serverCall.getMethodDescriptor).thenReturn(serverMethodDesc)
    when(serverMethodDesc.getFullMethodName).thenReturn("TestService/Test")

    val delegate = mock(classOf[Listener[Int]])
    val next = mock(classOf[ServerCallHandler[Int, Int]])
    when(next.startCall(any(classOf[ServerCall[Int, Int]]), any(classOf[Metadata]))).thenReturn(delegate)

    val listener = router.interceptCall(serverCall, new Metadata(), next)
    listener.onReady()
    listener.onMessage(1)
    listener.onHalfClose()
    listener.onComplete()
    listener.onCancel()

    verify(delegate, times(1)).onReady()
    if (isForwarded) {
      verify(delegate, times(0)).onMessage(1)
      verify(delegate, times(0)).onHalfClose()

      verify(serverCall, times(1)).sendHeaders(any(classOf[Metadata]))
      verify(serverCall, times(1)).sendMessage(1)
    } else if (isHandledLocally) {
      verify(delegate, times(1)).onMessage(1)
      verify(delegate, times(1)).onHalfClose()
    } else {
      verify(serverCall, times(0)).sendHeaders(any(classOf[Metadata]))
      verify(serverCall, times(0)).sendMessage(1)
      verify(serverCall, times(1)).close(any(classOf[Status]), any(classOf[Metadata]))
    }
    verify(delegate, times(1)).onComplete()
    verify(delegate, times(1)).onCancel()
  }
}
