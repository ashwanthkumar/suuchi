package in.ashwanthkumar.suuchi.rpc

import com.twitter.algebird.{Aggregator, LongRing, Semigroup}
import in.ashwanthkumar.suuchi.examples.rpc.generated.{AggregateRequest, AggregateResponse, AggregatorGrpc}
import in.ashwanthkumar.suuchi.router.Aggregation

import scala.concurrent.Future

class SuuchiAggregatorService extends AggregatorGrpc.Aggregator {
  override def aggregate(request: AggregateRequest) = Future.successful(AggregateResponse(output = 1))
}

class SumOfNumbers extends Aggregation {
  override def aggregator[ReqT, RespT] = {
    case AggregatorGrpc.METHOD_AGGREGATE => new Aggregator[AggregateResponse, Long, AggregateResponse] {
      override def prepare(input: AggregateResponse): Long = input.output
      override def semigroup: Semigroup[Long] = LongRing
      override def present(reduced: Long): AggregateResponse = AggregateResponse(output = reduced)
    }.asInstanceOf[Aggregator[RespT, Any, RespT]]
  }
}
