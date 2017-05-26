package in.ashwanthkumar.suuchi.router

import io.grpc.Metadata

object Headers {
  val ELIGIBLE_NODES = "eligible_nodes"
  val REPLICATION_REQUEST = "replication_request"
  val BROADCAST_REQUEST = "broadcast_request"

  val REPLICATION_REQUEST_KEY = Metadata.Key.of(Headers.REPLICATION_REQUEST, StringMarshaller)
  val ELIGIBLE_NODES_KEY = Metadata.Key.of(Headers.ELIGIBLE_NODES, MemberAddressMarshaller)
  val BROADCAST_REQUEST_KEY = Metadata.Key.of(Headers.BROADCAST_REQUEST, BooleanMarshaller)
}
