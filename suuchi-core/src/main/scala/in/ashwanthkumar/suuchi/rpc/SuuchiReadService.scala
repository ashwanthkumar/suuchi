package in.ashwanthkumar.suuchi.rpc

import com.google.protobuf.ByteString
import in.ashwanthkumar.suuchi.rpc.generated.SuuchiRPC.{GetRequest, GetResponse}
import in.ashwanthkumar.suuchi.rpc.generated.SuuchiRPC
import in.ashwanthkumar.suuchi.rpc.generated.SuuchiReadGrpc
import in.ashwanthkumar.suuchi.store.ReadStore
import io.grpc.stub.StreamObserver

class SuuchiReadService(store: ReadStore) extends SuuchiReadGrpc.SuuchiReadImplBase {
  override def get(request: GetRequest, responseObserver: StreamObserver[GetResponse]): Unit = {
    val key = request.getKey.toByteArray
    store.get(key) match {
      case Some(value) =>
        responseObserver.onNext(
          SuuchiRPC.GetResponse
            .newBuilder()
            .setKey(ByteString.copyFrom(key))
            .setValue(ByteString.copyFrom(value))
            .build()
        )
        responseObserver.onCompleted()
      case None =>
        responseObserver.onNext(
          SuuchiRPC.GetResponse
            .newBuilder()
            .setKey(ByteString.copyFrom(key))
            .build()
        )
        responseObserver.onCompleted()
    }
  }
}
