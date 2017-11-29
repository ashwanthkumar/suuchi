package in.ashwanthkumar.suuchi.rpc

import in.ashwanthkumar.suuchi.examples.rpc.generated.{ScanRequest, ScanResponse}
import in.ashwanthkumar.suuchi.store.InMemoryStore
import io.grpc.stub.ServerCallStreamObserver
import org.mockito.ArgumentCaptor
import org.mockito.Mockito._
import org.scalatest.FlatSpec
import org.scalatest.Matchers.{be, convertToAnyShouldWrapper, have}

import scala.collection.JavaConversions._

class SuuchiScanServiceTest extends FlatSpec {

  "SuuchiScanService" should "support scan for a given token range" in {

    val service = new SuuchiScanService(getPopulatedStore(10))

    val request = ScanRequest(start = Integer.MIN_VALUE, end = Integer.MAX_VALUE)

    val observer = mock(classOf[ServerCallStreamObserver[ScanResponse]])
    val runnable = ArgumentCaptor.forClass(classOf[Runnable])
    when(observer.isReady).thenReturn(true)
    service.scan(request, observer)
    verify(observer, times(1)).setOnReadyHandler(runnable.capture())
    runnable.getValue.run() // run the stream observer

    val captor = ArgumentCaptor.forClass(classOf[ScanResponse])
    val values = captor.getAllValues
    verify(observer, times(10)).onNext(captor.capture())
    verify(observer, times(1)).onCompleted()

    values should have size 10
    values.toList.map(extractKey).toSet should be(1 to 10 toSet)
  }

  it should "not include key which are out of the given token range" in {
    val service = new SuuchiScanService(getPopulatedStore(10))
    val request = ScanRequest(start = 1, end = 10)

    val observer = mock(classOf[ServerCallStreamObserver[ScanResponse]])
    val runnable = ArgumentCaptor.forClass(classOf[Runnable])
    when(observer.isReady).thenReturn(true)
    service.scan(request, observer)
    verify(observer, times(1)).setOnReadyHandler(runnable.capture())
    runnable.getValue.run() // run the stream observer

    val captor = ArgumentCaptor.forClass(classOf[ScanResponse])
    val values = captor.getAllValues
    when(observer.isReady).thenReturn(true)

    verify(observer, times(0)).onNext(captor.capture())
    verify(observer, times(1)).onCompleted()
    values should have size 0
  }

  private def getPopulatedStore(num: Int) = {
    val store = new InMemoryStore
    1 to num foreach (i => store.put(i.toString.getBytes, (i * 100).toString.getBytes))
    store
  }

  private def extractKey(response: ScanResponse) =
    new String(response.getKv.key.toByteArray).toInt
}
