package in.ashwanthkumar.suuchi.rpc

import com.twitter.algebird.{Aggregator, LongRing, Semigroup}
import in.ashwanthkumar.suuchi.router.Aggregation
import in.ashwanthkumar.suuchi.rpc.generated.SuuchiRPC.ReduceResponse
import in.ashwanthkumar.suuchi.rpc.generated.{AggregatorGrpc, SuuchiRPC}
import io.grpc.stub.StreamObserver

class SuuchiAggregatorService extends AggregatorGrpc.AggregatorImplBase {
  override def reduce(request: SuuchiRPC.ReduceRequest, responseObserver: StreamObserver[SuuchiRPC.ReduceResponse]) = {
    responseObserver.onNext(ReduceResponse.newBuilder().setOutput(1).build())
    responseObserver.onCompleted()
  }
}

class SumOfNumbers extends Aggregation {
  override def aggregator[ReqT, RespT] = {
    case AggregatorGrpc.METHOD_REDUCE => new Aggregator[ReduceResponse, Long, ReduceResponse] {
      override def prepare(input: ReduceResponse) = input.getOutput
      override def semigroup: Semigroup[Long] = LongRing
      override def present(reduced: Long) = ReduceResponse.newBuilder().setOutput(reduced).build()
    }.asInstanceOf[Aggregator[RespT, Any, RespT]]
  }
}