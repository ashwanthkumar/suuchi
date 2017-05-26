package in.ashwanthkumar.suuchi.rpc

import com.twitter.algebird.{Aggregator, LongRing, Semigroup}
import in.ashwanthkumar.suuchi.examples.rpc.generated.SuuchiRPC.AggregateResponse
import in.ashwanthkumar.suuchi.examples.rpc.generated.{AggregatorGrpc, SuuchiRPC}
import in.ashwanthkumar.suuchi.router.Aggregation
import io.grpc.stub.StreamObserver

class SuuchiAggregatorService extends AggregatorGrpc.AggregatorImplBase {
  override def aggregate(request: SuuchiRPC.AggregateRequest, responseObserver: StreamObserver[SuuchiRPC.AggregateResponse]) = {
    responseObserver.onNext(AggregateResponse.newBuilder().setOutput(1).build())
    responseObserver.onCompleted()
  }
}

class SumOfNumbers extends Aggregation {
  override def aggregator[ReqT, RespT] = {
    case AggregatorGrpc.METHOD_AGGREGATE => new Aggregator[AggregateResponse, Long, AggregateResponse] {
      override def prepare(input: AggregateResponse): Long = input.getOutput
      override def semigroup: Semigroup[Long] = LongRing
      override def present(reduced: Long): AggregateResponse = AggregateResponse.newBuilder().setOutput(reduced).build()
    }.asInstanceOf[Aggregator[RespT, Any, RespT]]
  }
}
