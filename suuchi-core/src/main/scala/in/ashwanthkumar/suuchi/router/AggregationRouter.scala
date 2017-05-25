package in.ashwanthkumar.suuchi.router

import java.util
import java.util.concurrent.TimeUnit

import com.google.common.util.concurrent.Futures
import com.google.protobuf.Message
import in.ashwanthkumar.suuchi.cluster.MemberAddress
import in.ashwanthkumar.suuchi.rpc.CachedChannelPool
import io.grpc._
import io.grpc.stub.ClientCalls
import com.twitter.algebird.Aggregator

class AggregationRouter(members: List[MemberAddress], self: MemberAddress, agg: (MethodDescriptor[Message, Message]) => Aggregator[Message, _, Message]) extends ServerInterceptor { me =>
  val channelPool = CachedChannelPool()

  override def interceptCall[ReqT, RespT](incomingRequest: ServerCall[ReqT, RespT], headers: Metadata, delegate: ServerCallHandler[ReqT, RespT]): ServerCall.Listener[ReqT] = {
    delegate.startCall(incomingRequest, headers)
  }
}

object AggregationRouter {
  def broadcast[ReqT, RespT](nodes: List[MemberAddress], channelPool: CachedChannelPool, methodDescriptor: MethodDescriptor[ReqT, RespT], input: ReqT): util.List[RespT] = {
    val scatterRequests = nodes.map(destination => {
      val channel = channelPool.get(destination, insecure = true)
      val clientCall = ClientInterceptors.interceptForward(channel)
        .newCall(methodDescriptor, CallOptions.DEFAULT.withDeadlineAfter(10, TimeUnit.MINUTES)) // TODO (ashwanthkumar): Make this deadline configurable
      ClientCalls.futureUnaryCall(clientCall, input)
    })

    Futures.allAsList(scatterRequests: _*).get()
  }
}

