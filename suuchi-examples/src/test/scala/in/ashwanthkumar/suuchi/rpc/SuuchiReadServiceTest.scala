package in.ashwanthkumar.suuchi.rpc

import com.google.protobuf.ByteString
import in.ashwanthkumar.suuchi.examples.rpc.generated.SuuchiRPC.{GetRequest, GetResponse}
import in.ashwanthkumar.suuchi.store.InMemoryStore
import io.grpc.stub.StreamObserver
import org.mockito.ArgumentCaptor
import org.mockito.Mockito._
import org.scalatest.FlatSpec
import org.scalatest.Matchers.{be, convertToAnyShouldWrapper}

class SuuchiReadServiceTest extends FlatSpec {
  val store = new InMemoryStore
  val service = new SuuchiReadService(store)

  "SuuchiReadService" should "support get on a value that exists in the store" in {
    store.put("1".getBytes, "2".getBytes) should be(true)
    val request = GetRequest.newBuilder().setKey(ByteString.copyFrom("1".getBytes)).build()

    val observer = mock(classOf[StreamObserver[GetResponse]])
    service.get(request, observer)

    verify(observer, times(1)).onCompleted()
    val captor = ArgumentCaptor.forClass(classOf[GetResponse])
    verify(observer, times(1)).onNext(captor.capture())
    val response = captor.getValue
    response.getKey should be(ByteString.copyFrom("1".getBytes))
    response.getValue should be(ByteString.copyFrom("2".getBytes))
  }

  it should "return response with empty value with key does not exist in store" in {
    val request = GetRequest.newBuilder().setKey(ByteString.copyFrom("2".getBytes)).build()
    val observer = mock(classOf[StreamObserver[GetResponse]])
    service.get(request, observer)
    verify(observer, times(1)).onCompleted()
    val captor = ArgumentCaptor.forClass(classOf[GetResponse])
    verify(observer, times(1)).onNext(captor.capture())
    val response = captor.getValue
    response.getKey should be(ByteString.copyFrom("2".getBytes))
    response.getValue.isEmpty should be(true)
  }
}
