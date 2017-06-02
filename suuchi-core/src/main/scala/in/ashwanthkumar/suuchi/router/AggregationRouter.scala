package in.ashwanthkumar.suuchi.router

import java.util
import java.util.concurrent.TimeUnit

import com.google.common.util.concurrent.Futures
import com.twitter.algebird.Aggregator
import in.ashwanthkumar.suuchi.cluster.MemberAddress
import in.ashwanthkumar.suuchi.rpc.CachedChannelPool
import io.grpc._
import io.grpc.stub.{ClientCalls, MetadataUtils, StreamObserver, StreamObservers}
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._

trait Aggregation {
  def aggregator[ReqT, RespT]: PartialFunction[MethodDescriptor[ReqT, RespT], Aggregator[RespT, Any, RespT]]
}

class AggregationRouter(members: List[MemberAddress], agg: Aggregation) extends ServerInterceptor {
  val channelPool = CachedChannelPool()
  val log = LoggerFactory.getLogger(classOf[AggregationRouter])

  override def interceptCall[ReqT, RespT](incomingRequest: ServerCall[ReqT, RespT], headers: Metadata, next: ServerCallHandler[ReqT, RespT]): ServerCall.Listener[ReqT] = {
    val isBroadcastRequest = headers.containsKey(Headers.BROADCAST_REQUEST_KEY)
    if (isBroadcastRequest || !agg.aggregator.isDefinedAt(incomingRequest.getMethodDescriptor)) {
      next.startCall(incomingRequest, headers)
    } else {
      // ServerCall.Listener for ServerStreaming methods
      headers.put(Headers.BROADCAST_REQUEST_KEY, true)
      incomingRequest.request(2)
      new ServerCall.Listener[ReqT] {
        val aggregator = agg.aggregator.apply(incomingRequest.getMethodDescriptor)
        var request: ReqT = _

        override def onCancel() = {
          log.debug("AggregationRouter#onCancel")
          incomingRequest.close(Status.CANCELLED, headers)
        }
        override def onHalfClose() = {
          log.debug("AggregationRouter#onHalfClose")
          try {
            val gathered = scatter(members, channelPool, incomingRequest.getMethodDescriptor, headers, request)
            val reduced = aggregator.apply(gathered.asScala)
            incomingRequest.sendHeaders(headers)
            incomingRequest.sendMessage(reduced)
            incomingRequest.close(Status.OK, headers)
          } catch {
            case e: Throwable =>
              log.error(e.getMessage, e)
              incomingRequest.close(Status.INTERNAL.withCause(e), headers)
          }
        }
        override def onReady() = {
          log.debug("AggregationRouter#onReady")
        }
        override def onMessage(message: ReqT) = {
          // We don't do the aggregation here but on onHalfClose()
          request = message
        }
        override def onComplete() = {
          log.debug("AggregationRouter#onComplete")
        }
      }
    }
  }

  protected def scatter[ReqT, RespT](nodes: List[MemberAddress], channelPool: CachedChannelPool, methodDescriptor: MethodDescriptor[ReqT, RespT], headers: Metadata, input: ReqT): util.List[RespT] = {
    AggregationRouter.scatter(nodes, channelPool, methodDescriptor, headers, input)
  }
}

object AggregationRouter {
  def scatter[ReqT, RespT](nodes: List[MemberAddress], channelPool: CachedChannelPool, methodDescriptor: MethodDescriptor[ReqT, RespT], headers:Metadata, input: ReqT): util.List[RespT] = {
    val scatterRequests = nodes.map(destination => {
      val channel = channelPool.get(destination, insecure = true)
      val clientCall = ClientInterceptors.interceptForward(channel, MetadataUtils.newAttachHeadersInterceptor(headers))
        .newCall(methodDescriptor, CallOptions.DEFAULT.withDeadlineAfter(10, TimeUnit.MINUTES)) // TODO (ashwanthkumar): Make this deadline configurable
      ClientCalls.futureUnaryCall(clientCall, input)
    })

    Futures.allAsList(scatterRequests: _*).get()
  }
}

