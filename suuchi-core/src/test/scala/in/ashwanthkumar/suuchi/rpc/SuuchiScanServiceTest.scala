package in.ashwanthkumar.suuchi.rpc

import in.ashwanthkumar.suuchi.rpc.generated.SuuchiRPC.{ScanRequest, ScanResponse}
import in.ashwanthkumar.suuchi.store.{InMemoryStore, Store}
import io.grpc.stub.ServerCallStreamObserver
import org.mockito.ArgumentCaptor
import org.mockito.Mockito._
import org.scalatest.FlatSpec
import org.scalatest.Matchers.{be, convertToAnyShouldWrapper, have}

import scala.collection.JavaConversions._

class SuuchiScanServiceTest extends FlatSpec {

  private def populateStore(num: Int, store: Store) = 1 to num foreach (i => store.put(i.toString.getBytes, (i * 100).toString.getBytes))

  private def extractKey(response: ScanResponse) = new String(response.getKv.getKey.toByteArray).toInt

  "SuuchiScanService" should "support scan for a given token range" in {

    val store = new InMemoryStore
    lazy val service = new SuuchiScanService(store)

    val request = ScanRequest.newBuilder()
      .setStart(Integer.MIN_VALUE)
      .setEnd(Integer.MAX_VALUE)
      .build()

    populateStore(10, store)

    val observer = mock(classOf[ServerCallStreamObserver[ScanResponse]])
    val captor = ArgumentCaptor.forClass(classOf[ScanResponse])
    val values = captor.getAllValues

    service.scan(request, observer)

    verify(observer, times(10)).onNext(captor.capture())
    verify(observer, times(1)).onCompleted()
    values should have size 10
    values.toList.map(extractKey).toSet should be(1 to 10 toSet)
  }

  it should "not include key which are out of the given toekn range" in {
    val store = new InMemoryStore
    lazy val service = new SuuchiScanService(store)

    val request = ScanRequest.newBuilder()
      .setStart(1)
      .setEnd(10)
      .build()

    populateStore(10, store)

    val observer = mock(classOf[ServerCallStreamObserver[ScanResponse]])
    val captor = ArgumentCaptor.forClass(classOf[ScanResponse])
    val values = captor.getAllValues

    service.scan(request, observer)

    verify(observer, times(0)).onNext(captor.capture())
    verify(observer, times(1)).onCompleted()
  }

}
