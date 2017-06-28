package in.ashwanthkumar.suuchi.router

import io.grpc.{Context, Metadata}

object Headers {
  val ELIGIBLE_NODES      = "eligible_nodes"
  val REPLICATION_REQUEST = "replication_request"
  val BROADCAST_REQUEST   = "broadcast_request"

  val REPLICATION_REQUEST_KEY = Metadata.Key.of(Headers.REPLICATION_REQUEST, StringMarshaller)

  /**
   * Context Key that's set to true if the node is processing a replication request (ie) a follower
   * or a slave. You might want to use this information
   *   - to avoid doing double counts etc.
   *   - to do certain tasks that should happen only at the primary replica etc.
   */
  val REPLICATION_REQUEST_CTX: Context.Key[Boolean] = Context.keyWithDefault(Headers.REPLICATION_REQUEST, false)

  val ELIGIBLE_NODES_KEY    = Metadata.Key.of(Headers.ELIGIBLE_NODES, MemberAddressMarshaller)
  val BROADCAST_REQUEST_KEY = Metadata.Key.of(Headers.BROADCAST_REQUEST, BooleanMarshaller)
}
