package in.ashwanthkumar.suuchi.rpc

import com.google.protobuf.ByteString
import in.ashwanthkumar.suuchi.rpc.generated.SuuchiRPC.{ScanRequest, ScanResponse}
import in.ashwanthkumar.suuchi.rpc.generated.{SuuchiRPC, SuuchiScanGrpc}
import in.ashwanthkumar.suuchi.store.ReadStore
import io.grpc.stub.StreamObserver

class SuuchiScanService(store: ReadStore) extends SuuchiScanGrpc.SuuchiScanImplBase {

  override def scan(request: ScanRequest, responseObserver: StreamObserver[ScanResponse]): Unit = {
    val from = request.getFrom.toByteArray
    val to = request.getTo.toByteArray

    store.scan(from, to) match {
      case Some(iterator) if iterator.hasNext => responseObserver.onNext{
        SuuchiRPC.ScanResponse.newBuilder()
          .setValues(ByteString.copyFrom(iterator.next()))
          .build()
      }
      case Some(iterator) if !iterator.hasNext => responseObserver.onCompleted()
      case None => responseObserver.onCompleted()
    }
  }

}
