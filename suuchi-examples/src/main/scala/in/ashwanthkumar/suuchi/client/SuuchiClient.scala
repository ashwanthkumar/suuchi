package in.ashwanthkumar.suuchi.client

import java.util.concurrent.{CountDownLatch, TimeUnit}

import com.google.protobuf.ByteString
import in.ashwanthkumar.suuchi.rpc.generated.SuuchiRPC.{PutResponse, GetRequest, PutRequest}
import in.ashwanthkumar.suuchi.rpc.generated.{SuuchiRPC, SuuchiPutGrpc, SuuchiReadGrpc}
import in.ashwanthkumar.suuchi.utils.Logging
import io.grpc.{ClientCall, CallOptions}
import io.grpc.netty.NettyChannelBuilder
import io.grpc.stub.{ClientCallStreamObserver, ClientCalls, StreamObserver}
import org.slf4j.LoggerFactory

class SuuchiClient(host: String, port: Int) {
  private val log = LoggerFactory.getLogger(getClass)

  private val channel = NettyChannelBuilder.forAddress(host, port)
    .usePlaintext(true)
    .build()

  private val writeStub = SuuchiPutGrpc.newBlockingStub(channel)
  private val asyncWrite = SuuchiPutGrpc.newStub(channel)
  private val readStub = SuuchiReadGrpc.newBlockingStub(channel)

  def shutdown() = {
    channel.awaitTermination(5, TimeUnit.SECONDS)
  }

  def put(key: Array[Byte], value: Array[Byte]): Boolean = {
    log.info(s"Doing Put with key=${new String(key)} value=${new String(value)}")
    val request = PutRequest.newBuilder()
      .setKey(ByteString.copyFrom(key))
      .setValue(ByteString.copyFrom(value))
      .build()

    writeStub.put(request).getStatus
  }

  def bulkPut(responseObserver: StreamObserver[SuuchiRPC.PutResponse]) = {
    asyncWrite.bulkPut(responseObserver).asInstanceOf[ClientCallStreamObserver[PutRequest]]
  }

  def get(key: Array[Byte]): Option[Array[Byte]] = {
    log.info("Doing Get with key={}", new String(key))
    val request = GetRequest.newBuilder()
      .setKey(ByteString.copyFrom(key))
      .build()

    val response = readStub.get(request)
    if (response.getValue.isEmpty) {
      None
    } else {
      Some(response.getValue.toByteArray)
    }
  }
}

class BulkPutListener(countDownLatch: CountDownLatch) extends StreamObserver[PutResponse] with Logging {
  override def onNext(value: PutResponse): Unit = {
    log.info("Got response from server")
    println(value.getStatus)
  }
  override def onError(t: Throwable): Unit = {
    log.error(t.getMessage, t)
    countDownLatch.countDown()
  }
  override def onCompleted(): Unit = {
    log.info("BatchPut is complete")
    countDownLatch.countDown()
  }
}

object SuuchiClient extends App {
  private val log = LoggerFactory.getLogger(getClass)
  val client = new SuuchiClient("localhost", 5051)

    (0 until 5).foreach { index =>
      val status = client.put(Array((65 + index).toByte), Array((65 + index).toByte))
      log.info("Put Status={}", status)
    }

    (0 until 5).foreach { index =>
      val value = client.get(Array((65 + index).toByte))
      log.info("Got value={}", new String(value.get))
    }

  val finishLatch = new CountDownLatch(1)

  val serverCall: ClientCall[PutRequest, PutResponse] = client.channel.newCall(SuuchiPutGrpc.METHOD_BULK_PUT, CallOptions.DEFAULT)
  val batch = client.bulkPut(new BulkPutListener(finishLatch))
  log.info("Starting bulkPut")

  val startIndex: Int = 10
  val endIndex: Int = 20
  (startIndex until endIndex).foreach { index =>
    while(!batch.isReady) {
      log.trace("Sleeping for 1 sec while the batch is getting ready - " + batch.isReady)
      Thread.sleep(1000)
    }
    log.info("Putting " + new String(Array((65 + index).toByte)))
    batch.onNext(
      PutRequest.newBuilder()
        .setKey(ByteString.copyFrom(Array((65 + index).toByte)))
        .setValue(ByteString.copyFrom(Array((65 + index).toByte)))
        .build()
    )
    log.trace("" + batch.isReady)
  }
  batch.onCompleted()
  log.info("Completed bulkPut, waiting...")
  finishLatch.await()
  log.info("BulkPut Complete")

  (startIndex until endIndex).foreach { index =>
    val value = client.get(Array((65 + index).toByte))
    log.info("Got value={}", new String(value.get))
  }

  client.shutdown()
}
