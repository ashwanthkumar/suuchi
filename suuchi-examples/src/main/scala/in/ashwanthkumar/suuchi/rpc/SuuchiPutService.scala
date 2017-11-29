package in.ashwanthkumar.suuchi.rpc

import in.ashwanthkumar.suuchi.examples.rpc.generated.{PutGrpc, PutRequest, PutResponse}
import in.ashwanthkumar.suuchi.store.WriteStore

import scala.concurrent.Future

class SuuchiPutService(store: WriteStore) extends PutGrpc.Put {
  override def put(request: PutRequest) = Future.successful {
    val key   = request.key.toByteArray
    val value = request.key.toByteArray

    val status = store.put(key, value)
    PutResponse(status = status)
  }
}
