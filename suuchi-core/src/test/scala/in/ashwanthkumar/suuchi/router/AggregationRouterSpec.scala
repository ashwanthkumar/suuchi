package in.ashwanthkumar.suuchi.router

import java.util.{List => JList}

import com.twitter.algebird.Aggregator
import in.ashwanthkumar.suuchi.cluster.MemberAddress
import in.ashwanthkumar.suuchi.core.tests.{FooRequest, FooResponse, RandomGrpc}
import in.ashwanthkumar.suuchi.rpc.CachedChannelPool
import io.grpc.ServerCall.Listener
import io.grpc._
import io.grpc.testing.TestMethodDescriptors
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.{any, eq => mockEq}
import org.mockito.Mockito.{mock, times, verify, when}
import org.scalatest.FlatSpec
import org.scalatest.Matchers.{be, convertToAnyShouldWrapper}

import scala.collection.JavaConverters._

class AggregationRouterSpec extends FlatSpec {
  class RandomAggregation extends Aggregation {
    override def aggregator[A, B]
      : PartialFunction[MethodDescriptor[A, B], Aggregator[B, Any, B]] = {
      case RandomGrpc.METHOD_FOO =>
        Aggregator.const(FooResponse.defaultInstance).asInstanceOf[Aggregator[B, Any, B]]
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

    val headers = new Metadata()
    headers.put(Headers.BROADCAST_REQUEST_KEY, true)

    val delegate = mock(classOf[Listener[FooRequest]])
    val next     = mock(classOf[ServerCallHandler[FooRequest, FooResponse]])
    when(next.startCall(any(classOf[ServerCall[FooRequest, FooResponse]]), mockEq(headers)))
      .thenReturn(delegate)

    val listener = router.interceptCall(serverCall, headers, next)
    listener.onReady()
    listener.onMessage(FooRequest())
    listener.onHalfClose()
    listener.onComplete()
    listener.onCancel()

    verify(next, times(1)).startCall(mockEq(serverCall), mockEq(headers))
  }

  it should "scatter requests to all the nodes when aggregation is defined on the method" in {
    val router =
      new AggregationRouter(List(MemberAddress("host:1")),
                                       new RandomAggregation) {
        override protected def scatter[ReqT, RespT](nodes: List[MemberAddress], channelPool: CachedChannelPool, methodDescriptor: MethodDescriptor[ReqT, RespT], headers: Metadata, input: ReqT) = {
          List(FooResponse.defaultInstance.asInstanceOf[RespT]).asJava
        }
      }

    val serverCall = mock(classOf[ServerCall[FooRequest, FooResponse]])
    when(serverCall.getMethodDescriptor).thenReturn(RandomGrpc.METHOD_FOO)

    val headers = new Metadata()

    val delegate = mock(classOf[Listener[FooRequest]])
    val next     = mock(classOf[ServerCallHandler[FooRequest, FooResponse]])
    when(next.startCall(any(classOf[ServerCall[FooRequest, FooResponse]]), mockEq(headers)))
      .thenReturn(delegate)

    val listener = router.interceptCall(serverCall, headers, next)
    listener.onReady()
    listener.onMessage(FooRequest())
    listener.onHalfClose()
    // during onHalfClose
    verify(serverCall, times(1)).sendHeaders(mockEq(headers))
    verify(serverCall, times(1)).sendMessage(mockEq(FooResponse.defaultInstance))
    verify(serverCall, times(1)).close(mockEq(Status.OK), mockEq(headers))

    listener.onComplete()

    listener.onCancel()
    // during onCancel
    verify(serverCall, times(1)).close(mockEq(Status.CANCELLED), mockEq(headers))

    // general interactions
    verify(next, times(0)).startCall(mockEq(serverCall), mockEq(headers))
    verify(serverCall, times(1)).request(2)
    headers.containsKey(Headers.BROADCAST_REQUEST_KEY) should be(true)
  }

  it should "fail with INTERNAL when scatter request fail" in {
    val router =
      new AggregationRouter(List(MemberAddress("host:1")), new RandomAggregation) {
        override protected def scatter[ReqT, RespT](
            nodes: List[MemberAddress],
            channelPool: CachedChannelPool,
            methodDescriptor: MethodDescriptor[ReqT, RespT],
            headers: Metadata,
            input: ReqT) = {
          throw new RuntimeException("scatter failed")
        }
      }

    val serverCall = mock(classOf[ServerCall[FooRequest, FooResponse]])
    when(serverCall.getMethodDescriptor).thenReturn(RandomGrpc.METHOD_FOO)

    val headers = new Metadata()

    val delegate = mock(classOf[Listener[FooRequest]])
    val next     = mock(classOf[ServerCallHandler[FooRequest, FooResponse]])
    when(next.startCall(any(classOf[ServerCall[FooRequest, FooResponse]]), mockEq(headers)))
      .thenReturn(delegate)

    val listener = router.interceptCall(serverCall, headers, next)
    listener.onReady()
    listener.onMessage(FooRequest())
    listener.onHalfClose()
    // during onHalfClose
    val statusCaptor = ArgumentCaptor.forClass(classOf[Status])
    verify(serverCall, times(1)).close(statusCaptor.capture(), mockEq(headers))
    val status = statusCaptor.getValue
    status.getCode should be(Status.INTERNAL.getCode)
    status.getCause.getMessage should be("scatter failed")

    listener.onComplete()

    // general interactions
    verify(next, times(0)).startCall(mockEq(serverCall), mockEq(headers))
    verify(serverCall, times(1)).request(2)
    headers.containsKey(Headers.BROADCAST_REQUEST_KEY) should be(true)
  }

}
