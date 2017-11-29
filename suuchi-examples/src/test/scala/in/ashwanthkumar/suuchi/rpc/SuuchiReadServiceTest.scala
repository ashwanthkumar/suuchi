package in.ashwanthkumar.suuchi.rpc

import com.google.protobuf.ByteString
import in.ashwanthkumar.suuchi.examples.rpc.generated.{GetRequest, GetResponse}
import in.ashwanthkumar.suuchi.store.InMemoryStore
import io.grpc.stub.StreamObserver
import org.mockito.Mockito._
import org.scalatest.FlatSpec
import org.scalatest.Matchers.{be, convertToAnyShouldWrapper}
import org.scalatest.concurrent.ScalaFutures.whenReady

class SuuchiReadServiceTest extends FlatSpec {
  val store   = new InMemoryStore
  val service = new SuuchiReadService(store)

  "SuuchiReadService" should "support get on a value that exists in the store" in {
    store.put("1".getBytes, "2".getBytes) should be(true)
    val request = GetRequest(key = ByteString.copyFrom("1".getBytes))

    val observer = mock(classOf[StreamObserver[GetResponse]])
    whenReady(service.get(request)) { response =>
      response.key should be(ByteString.copyFrom("1".getBytes))
      response.value should be(ByteString.copyFrom("2".getBytes))
    }
  }

  it should "return response with empty value with key does not exist in store" in {
    val request = GetRequest(key = ByteString.copyFrom("2".getBytes))
    whenReady(service.get(request)) { response =>
      response.key should be(ByteString.copyFrom("2".getBytes))
      response.value.isEmpty should be(true)
    }
  }
}
