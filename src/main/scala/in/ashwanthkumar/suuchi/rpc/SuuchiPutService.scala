package in.ashwanthkumar.suuchi.rpc

import com.google.protobuf.ByteString
import in.ashwanthkumar.suuchi.rpc.SuuchiRPC.{PutResponse, PutRequest, GetRequest, GetResponse}
import in.ashwanthkumar.suuchi.store.{WriteStore, ReadStore}
import io.grpc.stub.StreamObserver

class SuuchiPutService(store: WriteStore) extends SuuchiPutGrpc.SuuchiPutImplBase {
  override def put(request: PutRequest, responseObserver: StreamObserver[PutResponse]): Unit = {
    val key = request.getKey.toByteArray
    val value = request.getValue.toByteArray

    val status = store.put(key, value)
    responseObserver.onNext(PutResponse.newBuilder().setStatus(status).build())
    responseObserver.onCompleted()
  }
}
