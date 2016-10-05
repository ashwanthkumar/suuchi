package in.ashwanthkumar.suuchi.rpc

import in.ashwanthkumar.suuchi.rpc.generated.PingServiceGrpc
import in.ashwanthkumar.suuchi.rpc.generated.SuuchiRPC.{PingRequest, PingResponse}
import io.grpc.stub.StreamObserver

class PingService extends PingServiceGrpc.PingServiceImplBase {
  override def ping(request: PingRequest, responseObserver: StreamObserver[PingResponse]): Unit = {
    responseObserver.onNext(PingResponse.newBuilder().setStatus(true).build())
    responseObserver.onCompleted()
  }
}
