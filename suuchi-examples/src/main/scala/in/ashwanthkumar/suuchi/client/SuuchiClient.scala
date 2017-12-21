package in.ashwanthkumar.suuchi.client

import java.util.concurrent.TimeUnit

import com.google.protobuf.ByteString
import in.ashwanthkumar.suuchi.examples.rpc.generated._
import io.grpc.netty.NettyChannelBuilder
import org.slf4j.LoggerFactory

class SuuchiClient(host: String, port: Int) {
  private val log = LoggerFactory.getLogger(getClass)

  private val channel = NettyChannelBuilder
    .forAddress(host, port)
    .usePlaintext(true)
    .build()

  private val writeStub = PutGrpc.blockingStub(channel)
  private val readStub  = ReadGrpc.blockingStub(channel)
  private val scanStub  = ScanGrpc.blockingStub(channel)
  private val aggStub   = AggregatorGrpc.blockingStub(channel)

  def shutdown() = {
    channel.awaitTermination(5, TimeUnit.SECONDS)
  }

  def put(key: Array[Byte], value: Array[Byte]): Boolean = {
    log.info(s"Doing Put with key=${new String(key)} value=${new String(value)}")
    val request = PutRequest(key = ByteString.copyFrom(key), value = ByteString.copyFrom(value))
    writeStub.put(request).status
  }

  def get(key: Array[Byte]): Option[Array[Byte]] = {
    log.info("Doing Get with key={}", new String(key))
    val request = GetRequest(key = ByteString.copyFrom(key))

    val response = readStub.get(request)
    if (response.value.isEmpty) {
      None
    } else {
      Some(response.value.toByteArray)
    }
  }

  def scan() = {
    scanStub.scan(ScanRequest(start = Int.MinValue, end = Int.MaxValue))
  }

  def sumOfNumbers() = {
    aggStub.aggregate(AggregateRequest())
  }
}

object SuuchiClient extends App {
  private val log = LoggerFactory.getLogger(getClass)
  val client      = new SuuchiClient("localhost", 5051)

  (0 until 5).foreach { index =>
    val status = client.put(Array((65 + index).toByte), Array((65 + index).toByte))
    log.info("Put Status={}", status)
  }

  (0 until 5).foreach { index =>
    val value = client.get(Array((65 + index).toByte))
    log.info("Got value={}", new String(value.get))
  }

  (0 to 5).foreach { i =>
    client.put(s"prefix/$i".getBytes, s"$i".getBytes)
  }

  val iterator = client.scan()

  iterator.foreach { response =>
    println(new String(response.getKv.key.toByteArray))
  }

  println(client.sumOfNumbers)

  client.shutdown()
}
