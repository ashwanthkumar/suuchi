package in.ashwanthkumar.suuchi.rpc

import in.ashwanthkumar.suuchi.examples.rpc.generated.{PingRequest, PingResponse, PingServiceGrpc}

import scala.concurrent.Future

class PingService extends PingServiceGrpc.PingService {
  override def ping(request: PingRequest) = Future.successful(PingResponse(status = true))
}
