package in.ashwanthkumar.suuchi.rpc

import java.net.InetAddress
import java.util.concurrent.Executor

import in.ashwanthkumar.suuchi.cluster.MemberAddress
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

  /**
   * Enable a custom [[ReplicationRouter]] implementation for replicating requests to this service. [[RoutingStrategy]]
   * defines the list of nodes we should replicate the request too.
   *
   * @param service Service to which we should hook into for replication.
   *                Generally these're Write services.
   * @param replicator [[ReplicationRouter]] implementation that does the actual replication
   * @param strategy  [[RoutingStrategy]] for deciding what nodes we should replicate to
   * @return  this object for chaining
   */
  def withReplication(service: ServerServiceDefinition, replicator: ReplicationRouter, strategy: RoutingStrategy): Server[T] = {
    val router = new HandleOrForwardRouter(strategy, whoami)
    serverBuilder.addService(ServerInterceptors.interceptForward(service, router, replicator))
    this
  }

  /**
   * Enable a custom [[ReplicationRouter]] implementation for replicating requests to this service. [[RoutingStrategy]]
   * defines the list of nodes we should replicate the request too.
   *
   * @param service Service to which we should hook into for replication.
   *                Generally these're Write services.
   * @param replicator [[ReplicationRouter]] implementation that does the actual replication
   * @param strategy  [[RoutingStrategy]] for deciding what nodes we should replicate to
   * @return  this object for chaining
   */
  def withReplication(service: BindableService, replicator: ReplicationRouter, strategy: RoutingStrategy): Server[T] = {
    withReplication(service.bindService(), replicator, strategy)
  }

  /**
   * Enable [[SequentialReplicator]] for replicating requests to this service. [[RoutingStrategy]]
   * defines the list of nodes we should replicate the request too.
   *
   * @param service Service to which we should hook into for replication.
   *                Generally these're Write services.
   * @param nrReplicas  Number of replicas to make for each request
   * @param strategy  [[RoutingStrategy]] for deciding what nodes we should replicate to
   * @return  this object for chaining
   */
  def withSequentialReplication(service: ServerServiceDefinition, nrReplicas: Int, strategy: RoutingStrategy): Server[T] = {
    withReplication(service, new SequentialReplicator(nrReplicas, whoami), strategy)
  }

  /**
   * Enable [[SequentialReplicator]] for replicating requests to this service. [[RoutingStrategy]]
   * defines the list of nodes we should replicate the request too.
   *
   * @param service Service to which we should hook into for replication.
   *                Generally these're Write services.
   * @param nrReplicas  Number of replicas to make for each request
   * @param strategy  [[RoutingStrategy]] for deciding what nodes we should replicate to
   * @return  this object for chaining
   */
  def withSequentialReplication(service: BindableService, nrReplicas: Int, strategy: RoutingStrategy): Server[T] = {
    withSequentialReplication(service.bindService(), nrReplicas, strategy)
  }

  /**
   * Enable [[ParallelReplicator]] for replicating requets to this service. [[RoutingStrategy]]
   * defines the list of nodes we should replicate the request too.
   *
   * @param service Service to which we should hook into for replication.
   *                Generally these're Write services.
   * @param nrReplicas  Number of replicas to make for each request
   * @param strategy  [[RoutingStrategy]] for deciding what nodes we should replicate to
   * @return  this object for chaining
   * @return
   */
  def withParallelReplication(service: ServerServiceDefinition, nrReplicas: Int, strategy: RoutingStrategy): Server[T] = {
    withReplication(service, new ParallelReplicator(nrReplicas, whoami), strategy)
  }

  /**
   * Enable [[ParallelReplicator]] for replicating requets to this service. [[RoutingStrategy]]
   * defines the list of nodes we should replicate the request too.
   *
   * @param service Service to which we should hook into for replication.
   *                Generally these're Write services.
   * @param nrReplicas  Number of replicas to make for each request
   * @param strategy  [[RoutingStrategy]] for deciding what nodes we should replicate to
   * @return  this object for chaining
   */
  def withParallelReplication(service: BindableService, nrReplicas: Int, strategy: RoutingStrategy): Server[T] = {
    withParallelReplication(service.bindService(), nrReplicas, strategy)
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


