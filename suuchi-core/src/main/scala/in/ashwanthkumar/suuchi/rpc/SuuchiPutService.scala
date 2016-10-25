package in.ashwanthkumar.suuchi.rpc

import com.google.protobuf.ByteString
import in.ashwanthkumar.suuchi.rpc.generated.SuuchiPutGrpc
import in.ashwanthkumar.suuchi.rpc.generated.SuuchiRPC.{PutResponse, PutRequest, GetRequest, GetResponse}
import in.ashwanthkumar.suuchi.store.{WriteStore, ReadStore}
import in.ashwanthkumar.suuchi.utils.Logging
import io.grpc.stub.StreamObserver

class SuuchiPutService(store: WriteStore) extends SuuchiPutGrpc.SuuchiPutImplBase with Logging {
  override def put(request: PutRequest, responseObserver: StreamObserver[PutResponse]): Unit = {
    val key = request.getKey.toByteArray
    val value = request.getValue.toByteArray

    val status = store.put(key, value)
    responseObserver.onNext(PutResponse.newBuilder().setStatus(status).build())
    responseObserver.onCompleted()
  }

  override def bulkPut(responseObserver: StreamObserver[PutResponse]): StreamObserver[PutRequest] = new StreamObserver[PutRequest] {
    var status = true
    override def onError(t: Throwable): Unit = {
      log.error(t.getMessage, t)
      responseObserver.onError(t)
    }
    override def onNext(value: PutRequest): Unit = {
      val k = value.getKey.toByteArray
      val v = value.getValue.toByteArray

      status = status & store.put(k, v)
    }
    override def onCompleted(): Unit = {
      responseObserver.onNext(PutResponse.newBuilder().setStatus(status).build())
      responseObserver.onCompleted()
    }
  }
}
