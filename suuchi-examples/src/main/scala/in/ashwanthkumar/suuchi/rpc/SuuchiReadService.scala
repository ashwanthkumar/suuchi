package in.ashwanthkumar.suuchi.rpc

import com.google.protobuf.ByteString
import in.ashwanthkumar.suuchi.examples.rpc.generated.{GetRequest, GetResponse, ReadGrpc}
import in.ashwanthkumar.suuchi.store.ReadStore

import scala.concurrent.Future

class SuuchiReadService(store: ReadStore) extends ReadGrpc.Read {

  override def get(request: GetRequest) = Future.successful {
    val key = request.key.toByteArray
    store.get(key) match {
      case Some(value) =>
        GetResponse(key = ByteString.copyFrom(key), value = ByteString.copyFrom(value))
      case None =>
        GetResponse(key = ByteString.copyFrom(key))
    }
  }
}
