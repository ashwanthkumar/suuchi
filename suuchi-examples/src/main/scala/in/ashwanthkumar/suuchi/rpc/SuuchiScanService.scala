package in.ashwanthkumar.suuchi.rpc

import com.google.protobuf.ByteString
import in.ashwanthkumar.suuchi.examples.rpc.generated.{KV, ScanGrpc, ScanRequest, ScanResponse}
import in.ashwanthkumar.suuchi.partitioner.SuuchiHash
import in.ashwanthkumar.suuchi.store.Store
import in.ashwanthkumar.suuchi.store.{KV => StoreKV}
import in.ashwanthkumar.suuchi.utils.ByteArrayUtils
import io.grpc.stub.{ServerCallStreamObserver, StreamObserver}

class SuuchiScanService(store: Store) extends ScanGrpc.Scan {

  override def scan(request: ScanRequest, responseObserver: StreamObserver[ScanResponse]): Unit = {
    val observer = responseObserver.asInstanceOf[ServerCallStreamObserver[ScanResponse]]
    val start    = request.start
    val end      = request.end

    val scanner = store.scanner()
    scanner.prepare()
    val it = scanner
      .scan()
      .filter(kv => ByteArrayUtils.isHashKeyWithinRange(start, end, kv.key, SuuchiHash))
      .map(buildResponse)

    observer.setOnCancelHandler(new Runnable() {
      override def run() = {
        scanner.close()
      }
    })
    observer.setOnReadyHandler(new Runnable() {
      override def run() = {
        while (observer.isReady && it.hasNext) {
          observer.onNext(it.next)
        }

        if (!it.hasNext) {
          observer.onCompleted()
          scanner.close()
        }
      }
    })
  }

  private def buildKV(kv: StoreKV) = {
    KV(key = ByteString.copyFrom(kv.key), value = ByteString.copyFrom(kv.value))
  }

  private def buildResponse(response: StoreKV): ScanResponse = {
    ScanResponse(kv = Option(buildKV(response)))
  }
}
