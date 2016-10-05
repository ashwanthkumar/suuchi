package in.ashwanthkumar.suuchi.rpc

import in.ashwanthkumar.suuchi.rpc.generated.SuuchiRPC.{PingRequest, PingResponse}
import io.grpc.stub.StreamObserver
import org.mockito.ArgumentCaptor
import org.mockito.Mockito._
import org.scalatest.FlatSpec
import org.scalatest.Matchers.{be, convertToAnyShouldWrapper}

class PingServiceTest extends FlatSpec {
  val service = new PingService

  "PingService" should "return true when pinged" in {
    val request = PingRequest.newBuilder().build()
    val observer = mock(classOf[StreamObserver[PingResponse]])
    service.ping(request, observer)
    verify(observer, times(1)).onCompleted()
    val captor = ArgumentCaptor.forClass(classOf[PingResponse])
    verify(observer, times(1)).onNext(captor.capture())

    val response = captor.getValue
    response.getStatus should be(true)
  }
}
