package in.ashwanthkumar.suuchi.router

import com.twitter.algebird.{Aggregator, MonoidAggregator}
import in.ashwanthkumar.suuchi.cluster.MemberAddress
import in.ashwanthkumar.suuchi.core.tests.SuuchiTestRPC.{FooRequest, FooResponse}
import in.ashwanthkumar.suuchi.core.tests.{RandomGrpc, SuuchiTestRPC}
import io.grpc.ServerCall.Listener
import io.grpc.{Metadata, MethodDescriptor, ServerCall, ServerCallHandler}
import io.grpc.testing.TestMethodDescriptors
import org.mockito.Matchers.{any, eq => mockEq}
import org.mockito.Mockito.{mock, times, verify, when}
import org.scalatest.FlatSpec
import org.scalatest.Matchers.{be, convertToAnyShouldWrapper}

class AggregationRouterSpec extends FlatSpec {
  class RandomAggregation extends Aggregation {
    override def aggregator[A, B]
      : PartialFunction[MethodDescriptor[A, B], Aggregator[B, Any, B]] = {
      case RandomGrpc.METHOD_FOO => Aggregator.const(1l).asInstanceOf[Aggregator[B, Any, B]]
    }
  }

  "AggregationRouter" should "not do if the aggregation is not defined for the method" in {
    val router =
      new AggregationRouter(List(MemberAddress("host:1")), new RandomAggregation)
    val serverCall       = mock(classOf[ServerCall[Int, Int]])
    val serverMethodDesc = TestMethodDescriptors.noopMethod[Int, Int]()
    when(serverCall.getMethodDescriptor).thenReturn(serverMethodDesc)

    val headers  = new Metadata
    val delegate = mock(classOf[Listener[Int]])
    val next     = mock(classOf[ServerCallHandler[Int, Int]])
    when(next.startCall(any(classOf[ServerCall[Int, Int]]), mockEq(headers)))
      .thenReturn(delegate)

    val listener = router.interceptCall(serverCall, headers, next)
    listener.onReady()
    listener.onMessage(1)
    listener.onHalfClose()
    listener.onComplete()
    listener.onCancel()

    verify(next, times(1)).startCall(mockEq(serverCall), mockEq(headers))
  }

  it should "not scatter requests if it already has BROADCAST header" in {
    val router =
      new AggregationRouter(List(MemberAddress("host:1")), new RandomAggregation)

    val serverCall       = mock(classOf[ServerCall[FooRequest, FooResponse]])
    val serverMethodDesc = TestMethodDescriptors.noopMethod[FooRequest, FooResponse]()
    when(serverCall.getMethodDescriptor).thenReturn(serverMethodDesc)

    val headers  = new Metadata()
    headers.put(Headers.BROADCAST_REQUEST_KEY, true)

    val delegate = mock(classOf[Listener[FooRequest]])
    val next     = mock(classOf[ServerCallHandler[FooRequest, FooResponse]])
    when(next.startCall(any(classOf[ServerCall[FooRequest, FooResponse]]), mockEq(headers)))
      .thenReturn(delegate)

    val listener = router.interceptCall(serverCall, headers, next)
    listener.onReady()
    listener.onMessage(FooRequest.newBuilder().build())
    listener.onHalfClose()
    listener.onComplete()
    listener.onCancel()

    verify(next, times(1)).startCall(mockEq(serverCall), mockEq(headers))
  }

}
