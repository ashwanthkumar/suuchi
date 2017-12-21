package in.ashwanthkumar.suuchi.example

import java.util.concurrent.Executors

import in.ashwanthkumar.suuchi.examples.rpc.generated.{AggregatorGrpc, PutGrpc, ReadGrpc, ScanGrpc}
import in.ashwanthkumar.suuchi.router.ConsistentHashingRouting
import in.ashwanthkumar.suuchi.rpc.Server.whoami
import in.ashwanthkumar.suuchi.rpc._
import in.ashwanthkumar.suuchi.store.InMemoryStore
import io.grpc.netty.NettyServerBuilder

import scala.concurrent.ExecutionContext

// Start the app with either / one each of 5051, 5052 or/and 5053 port numbers
object DistributedKVServer extends App {

  val port                = args(0).toInt
  val PARTITIONS_PER_NODE = 100
  val REPLICATION_FACTOR  = 2

  val allNodes = List(whoami(5051), whoami(5052), whoami(5053))
  val routingStrategy =
    ConsistentHashingRouting(REPLICATION_FACTOR, PARTITIONS_PER_NODE, allNodes: _*)

  val cachedThreadPool = Executors.newCachedThreadPool()
  val executionContext = ExecutionContext.fromExecutor(cachedThreadPool)

  val store = new InMemoryStore
  val server = Server(NettyServerBuilder.forPort(port), whoami(port))
    .routeUsing(ReadGrpc.bindService(new SuuchiReadService(store), executionContext), routingStrategy)
    .withParallelReplication(PutGrpc.bindService(new SuuchiPutService(store), executionContext), REPLICATION_FACTOR, routingStrategy)
    .withService(ScanGrpc.bindService(new SuuchiScanService(store), executionContext))
    .aggregate(allNodes, AggregatorGrpc.bindService(new SuuchiAggregatorService(), executionContext), new SumOfNumbers)

  server.start()

  server.blockUntilShutdown()
}
