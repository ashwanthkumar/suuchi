package in.ashwanthkumar.suuchi.router

import java.util.concurrent.Executor

import com.google.common.util.concurrent.{Futures, ListenableFuture}
import in.ashwanthkumar.suuchi.membership.MemberAddress
import in.ashwanthkumar.suuchi.router.replication.ParallelReplicator
import in.ashwanthkumar.suuchi.rpc.CachedChannelPool
import io.grpc.{Metadata, MethodDescriptor, ServerCall}
import org.mockito.Matchers
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfter, FlatSpec}

class MockParallelReplicator[ReqT](config: ReplicatorConfig, channelPool: CachedChannelPool, mock: ParallelReplicator[ReqT])(implicit executor: Executor) extends ParallelReplicator[ReqT](config, channelPool) {
  override def forwardAsync[RespT](methodDescriptor: MethodDescriptor[ReqT, RespT], headers: Metadata,
                                         incomingRequest: ReqT, destination: MemberAddress): Unit = {
    mock.forwardAsync[RespT](methodDescriptor, headers, incomingRequest, destination)
  }
}

class ParallelReplicatorSpec extends FlatSpec {
  "ParallelReplicator" should "forward requests to target nodes in parallel" in {
    implicit val mockExecutor = mock(classOf[Executor])
    val mockReplicator = mock(classOf[ParallelReplicator[Int]])
    val mockChannelPool = mock(classOf[CachedChannelPool])
    val replicator = new MockParallelReplicator(ReplicatorConfig(2, MemberAddress("host1", 1), new Metadata()), mockChannelPool, mockReplicator)
    val serverCall = mock(classOf[ServerCall[Int, Int]])
    val delegate = mock(classOf[ServerCall.Listener[Int]])
    val headers = new Metadata()
    val destination1 = MemberAddress("host2", 2)
    val destination2 = MemberAddress("host3", 3)

    when(mockReplicator.forwardAsync(any(classOf[MethodDescriptor[Int, Int]]), any(classOf[Metadata]), anyInt(), Matchers.eq(destination1)))
//      .thenReturn(Futures.immediateFuture(2))
    when(mockReplicator.forwardAsync(any(classOf[MethodDescriptor[Int, Int]]), any(classOf[Metadata]), anyInt(), Matchers.eq(destination2)))
//      .thenReturn(Futures.immediateFuture(3))

    replicator.replicate[Int](List(destination1, destination2), 1, serverCall, delegate)

    verify(mockReplicator, times(1)).forwardAsync(any(classOf[MethodDescriptor[Int, Int]]), any(classOf[Metadata]), anyInt(), Matchers.eq(destination1))
    verify(mockReplicator, times(1)).forwardAsync(any(classOf[MethodDescriptor[Int, Int]]), any(classOf[Metadata]), anyInt(), Matchers.eq(destination2))

  }

  it should "forward requests to target nodes in parallel & once done, should delegate to local node if it's in the replica node list" in {
    implicit val mockExecutor = mock(classOf[Executor])
    val mockReplicator = mock(classOf[ParallelReplicator[Int]])
    val mockChannelPool = mock(classOf[CachedChannelPool])
    val replicator = new MockParallelReplicator(ReplicatorConfig(2, MemberAddress("host1", 1), new Metadata()), mockChannelPool, mockReplicator)
    val serverCall = mock(classOf[ServerCall[Int, Int]])
    val delegate = mock(classOf[ServerCall.Listener[Int]])
    val headers = new Metadata()
    val destination1 = MemberAddress("host2", 2)
    val destination2 = MemberAddress("host1", 1)

    when(mockReplicator.forwardAsync(any(classOf[MethodDescriptor[Int, Int]]), any(classOf[Metadata]), anyInt(), Matchers.eq(destination1)))
//      .thenReturn(Futures.immediateFuture(2))

    replicator.replicate[Int](List(destination1, destination2), 1, serverCall, delegate)

    verify(mockReplicator, times(1)).forwardAsync(any(classOf[MethodDescriptor[Int, Int]]), any(classOf[Metadata]), anyInt(), Matchers.eq(destination1))
    verify(delegate, times(1)).onMessage(Matchers.eq(1))

  }
}
