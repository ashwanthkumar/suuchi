package in.ashwanthkumar.suuchi.rpc

import in.ashwanthkumar.suuchi.examples.rpc.generated.{PingRequest, PingResponse}
import io.grpc.stub.StreamObserver
import org.mockito.Mockito._
import org.scalatest.FlatSpec
import org.scalatest.Matchers.{be, convertToAnyShouldWrapper}
import org.scalatest.concurrent.ScalaFutures.whenReady

class PingServiceTest extends FlatSpec {
  val service = new PingService

  "PingService" should "return true when pinged" in {
    val request  = PingRequest()
    val observer = mock(classOf[StreamObserver[PingResponse]])
    whenReady(service.ping(request)) { response =>
      response.status should be(true)
    }
  }
}
