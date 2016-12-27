package in.ashwanthkumar.suuchi.utils

import io.grpc.stub.ServerCallStreamObserver

object ConnectionUtils {
  def waitForReady[T](observer: ServerCallStreamObserver[T]) = {
    while (!observer.isReady) {
      Thread.sleep(1000)
    }
  }
}
