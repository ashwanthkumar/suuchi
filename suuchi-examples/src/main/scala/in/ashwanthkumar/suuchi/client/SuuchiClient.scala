package in.ashwanthkumar.suuchi.client

import java.util.concurrent.TimeUnit

import com.google.protobuf.ByteString
import in.ashwanthkumar.suuchi.examples.rpc.generated.SuuchiRPC._
import in.ashwanthkumar.suuchi.examples.rpc.generated._
import io.grpc.netty.NettyChannelBuilder
import org.slf4j.LoggerFactory

import scala.collection.JavaConversions._

class SuuchiClient(host: String, port: Int) {
  private val log = LoggerFactory.getLogger(getClass)

  private val channel = NettyChannelBuilder.forAddress(host, port)
    .usePlaintext(true)
    .build()

  private val writeStub = PutGrpc.newBlockingStub(channel)
  private val readStub = ReadGrpc.newBlockingStub(channel)
  private val scanStub = ScanGrpc.newBlockingStub(channel)
  private val aggStub = AggregatorGrpc.newBlockingStub(channel)

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

  def scan() = {
    scanStub.scan(ScanRequest.newBuilder().setStart(Int.MinValue).setEnd(Int.MaxValue).build())
  }

  def sumOfNumbers() = {
    aggStub.aggregate(AggregateRequest.newBuilder().build())
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

  (0 to 5).foreach{ i =>
    client.put(s"prefix/$i".getBytes, s"$i".getBytes)
  }

  val iterator = client.scan()

  iterator.foreach{ response =>
    println(new String(response.getKv.getKey.toByteArray))
  }

  println(client.sumOfNumbers)

  client.shutdown()
}
