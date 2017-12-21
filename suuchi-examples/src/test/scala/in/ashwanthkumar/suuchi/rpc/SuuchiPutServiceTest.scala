package in.ashwanthkumar.suuchi.rpc

import com.google.protobuf.ByteString
import in.ashwanthkumar.suuchi.examples.rpc.generated.PutRequest
import in.ashwanthkumar.suuchi.store.InMemoryStore
import org.scalatest.FlatSpec
import org.scalatest.Matchers.{be, convertToAnyShouldWrapper}
import org.scalatest.concurrent.ScalaFutures.whenReady

class SuuchiPutServiceTest extends FlatSpec {
  val store   = new InMemoryStore
  val service = new SuuchiPutService(store)

  "SuuchiPutService" should "support put for a Key-Value to the store" in {
    val request = PutRequest(key = ByteString.copyFrom("1".getBytes), value = ByteString.copyFrom("2".getBytes))

    whenReady(service.put(request)) { response =>
      response.status should be(true)
    }
  }

}
