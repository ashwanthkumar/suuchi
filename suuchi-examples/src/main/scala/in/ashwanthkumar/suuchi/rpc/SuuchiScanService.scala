package in.ashwanthkumar.suuchi.rpc

import com.google.protobuf.ByteString
import in.ashwanthkumar.suuchi.examples.rpc.generated.SuuchiRPC.{ScanRequest, ScanResponse}
import in.ashwanthkumar.suuchi.examples.rpc.generated.{ScanGrpc, SuuchiRPC}
import in.ashwanthkumar.suuchi.partitioner.SuuchiHash
import in.ashwanthkumar.suuchi.store.{KV, Store}
import in.ashwanthkumar.suuchi.utils.{ByteArrayUtils, ConnectionUtils}
import io.grpc.stub.{ServerCallStreamObserver, StreamObserver, StreamObservers}
import scala.collection.JavaConverters._

class SuuchiScanService(store: Store) extends ScanGrpc.ScanImplBase with ConnectionUtils {

  private def buildResponse(response: KV): ScanResponse = {
    SuuchiRPC.ScanResponse
      .newBuilder()
      .setKv(buildKV(response))
      .build()
  }

  override def scan(request: ScanRequest, responseObserver: StreamObserver[ScanResponse]): Unit = {
    val observer = responseObserver.asInstanceOf[ServerCallStreamObserver[ScanResponse]]
    val start    = request.getStart
    val end      = request.getEnd

    val it = store
      .scan()
      .filter(kv => ByteArrayUtils.isHashKeyWithinRange(start, end, kv.key, SuuchiHash))
      .map(buildResponse)
    StreamObservers.copyWithFlowControl[ScanResponse](it.asJava, observer)
  }

  private def buildKV(kv: KV) = {
    SuuchiRPC.KV
      .newBuilder()
      .setKey(ByteString.copyFrom(kv.key))
      .setValue(ByteString.copyFrom(kv.value))
      .build()
  }
}
