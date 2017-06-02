package in.ashwanthkumar.suuchi.rpc

import in.ashwanthkumar.suuchi.examples.rpc.generated.PutGrpc
import in.ashwanthkumar.suuchi.examples.rpc.generated.SuuchiRPC.{PutRequest, PutResponse}
import in.ashwanthkumar.suuchi.store.WriteStore
import io.grpc.stub.StreamObserver

class SuuchiPutService(store: WriteStore) extends PutGrpc.PutImplBase {
  override def put(request: PutRequest, responseObserver: StreamObserver[PutResponse]): Unit = {
    val key   = request.getKey.toByteArray
    val value = request.getValue.toByteArray

    val status = store.put(key, value)
    responseObserver.onNext(PutResponse.newBuilder().setStatus(status).build())
    responseObserver.onCompleted()
  }
}
