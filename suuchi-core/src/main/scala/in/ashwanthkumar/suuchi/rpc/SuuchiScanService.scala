package in.ashwanthkumar.suuchi.rpc

import com.google.protobuf.ByteString
import in.ashwanthkumar.suuchi.partitioner.SuuchiHash
import in.ashwanthkumar.suuchi.rpc.generated.SuuchiRPC.{ScanRequest, ScanResponse}
import in.ashwanthkumar.suuchi.rpc.generated.{SuuchiRPC, SuuchiScanGrpc}
import in.ashwanthkumar.suuchi.store.{KV, Store}
import in.ashwanthkumar.suuchi.utils.{ByteArrayUtils, ConnectionUtils}
import io.grpc.stub.{ServerCallStreamObserver, StreamObserver}

class SuuchiScanService(store: Store) extends SuuchiScanGrpc.SuuchiScanImplBase with ConnectionUtils {

  private def buildResponse(response: KV): ScanResponse = {
    SuuchiRPC.ScanResponse.newBuilder()
      .setKv(buildKV(response))
      .build()
  }

  override def scan(request: ScanRequest, responseObserver: StreamObserver[ScanResponse]): Unit = {
    val observer = responseObserver.asInstanceOf[ServerCallStreamObserver[ScanResponse]]
    val start = request.getStart
    val end = request.getEnd

    val scanner = store.scanner()
    scanner.prepare()
    val iterator = scanner.scan()
    for(response <- iterator) {
      //TODO: Is observer.isCancelled needed to checked before observer.onNext?
      if (ByteArrayUtils.isHashKeyWithinRange(start, end, response.key, SuuchiHash)) {
        exponentialBackoffTill(observer.isReady, message = "Waiting for client to get ready")
        observer.onNext(buildResponse(response))
      }
    }
    observer.onCompleted()
    scanner.close()
  }

  private def buildKV(kv: KV) = {
    SuuchiRPC.KV.newBuilder()
      .setKey(ByteString.copyFrom(kv.key))
      .setValue(ByteString.copyFrom(kv.value))
      .build()
  }
}
