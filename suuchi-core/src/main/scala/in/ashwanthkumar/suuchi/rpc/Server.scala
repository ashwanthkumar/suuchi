package in.ashwanthkumar.suuchi.rpc

import java.net.InetAddress

import in.ashwanthkumar.suuchi.membership.MemberAddress
import in.ashwanthkumar.suuchi.router.{HandleOrForwardRouter, RoutingStrategy, SequentialReplicator}
import io.grpc.{Server => GServer, _}
import org.slf4j.LoggerFactory

class Server[T <: ServerBuilder[T]](serverBuilder: ServerBuilder[T], whoami: MemberAddress) {
  private val log = LoggerFactory.getLogger(classOf[Server[_]])

  private var server: GServer = _

  def start() = {
    server = serverBuilder
      .build()
      .start()
    log.info("Server started, listening on " + server.getPort)
    Runtime.getRuntime.addShutdownHook(new Thread() {
      override def run() {
        // Use stderr here since the logger may have been reset by its JVM shutdown hook.
        System.err.println("*** shutting down gRPC server since JVM is shutting down")
        Server.this.stop()
        System.err.println("*** server shut down")
      }
    })
  }

  def withReplication(service: BindableService, nrOfReplica: Int, strategy: RoutingStrategy) = {
    val router = new HandleOrForwardRouter(strategy, whoami)
    // FIXME - make the Replicator pluggable
    val replicator = new SequentialReplicator(nrOfReplica, whoami)
    serverBuilder.addService(ServerInterceptors.interceptForward(service, router, replicator))
    this
  }

  def routeUsing(service: BindableService, strategy: RoutingStrategy): Server[T] = routeUsing(service.bindService(), strategy)

  def routeUsing(service: ServerServiceDefinition, strategy: RoutingStrategy) = {
    val router = new HandleOrForwardRouter(strategy, whoami)
    serverBuilder.addService(ServerInterceptors.interceptForward(service, router))
    this
  }

  def stop() = {
    if (server != null) {
      server.shutdown()
    }
  }

  def blockUntilShutdown() = {
    if (server != null) {
      server.awaitTermination()
    }
  }
}

object Server {
  def apply[T <: ServerBuilder[T]](serverBuilder: ServerBuilder[T], whoami: MemberAddress) = new Server[T](serverBuilder, whoami)

  /**
   * Helper to generate a MemberAddress that would identify the hostname of the node and use the given port
   * to return the identity of the current node as MemberAddress
   * @param port  Port where the server is meant to be running
   * @return  MemberAddress - Identity of the current SuuchiServer
   */
  def whoami(port: Int) = MemberAddress(InetAddress.getLocalHost.getCanonicalHostName, port)
}


