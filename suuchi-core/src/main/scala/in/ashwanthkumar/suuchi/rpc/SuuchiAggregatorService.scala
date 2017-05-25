package in.ashwanthkumar.suuchi.rpc

import in.ashwanthkumar.suuchi.rpc.generated.{AggregatorGrpc, SuuchiRPC}
import io.grpc.stub.StreamObserver

class SuuchiAggregatorService extends AggregatorGrpc.AggregatorImplBase {
  override def reduce(request: SuuchiRPC.ScatterRequest, responseObserver: StreamObserver[SuuchiRPC.ScatterResponse]): Unit = super.reduce(request, responseObserver)
}

