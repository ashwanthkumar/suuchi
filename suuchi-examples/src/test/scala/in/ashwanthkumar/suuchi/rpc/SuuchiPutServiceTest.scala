package in.ashwanthkumar.suuchi.rpc

import com.google.protobuf.ByteString
import in.ashwanthkumar.suuchi.examples.rpc.generated.SuuchiRPC.{PutRequest, PutResponse}
import in.ashwanthkumar.suuchi.store.InMemoryStore
import io.grpc.stub.StreamObserver
import org.mockito.ArgumentCaptor
import org.mockito.Mockito._
import org.scalatest.FlatSpec
import org.scalatest.Matchers.{be, convertToAnyShouldWrapper}

class SuuchiPutServiceTest extends FlatSpec {
  val store = new InMemoryStore
  val service = new SuuchiPutService(store)

  "SuuchiPutService" should "support put for a Key-Value to the store" in {
    val request = PutRequest.newBuilder()
      .setKey(ByteString.copyFrom("1".getBytes))
      .setValue(ByteString.copyFrom("2".getBytes))
      .build()

    val observer = mock(classOf[StreamObserver[PutResponse]])
    service.put(request, observer)
    verify(observer, times(1)).onCompleted()
    val captor = ArgumentCaptor.forClass(classOf[PutResponse])
    verify(observer, times(1)).onNext(captor.capture())

    val response = captor.getValue
    response.getStatus should be(true)
  }

}
