package in.ashwanthkumar.suuchi.router

import java.util.concurrent.Executor

import com.google.common.util.concurrent.{Futures, ListenableFuture}
import in.ashwanthkumar.suuchi.cluster.MemberAddress
import io.grpc.{Metadata, MethodDescriptor, ServerCall}
import org.mockito.Matchers
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfter, FlatSpec}

class MockParallelReplicator(nrReplicas: Int, self: MemberAddress, mock: ParallelReplicator) extends ParallelReplicator(nrReplicas, self) {
  override def forwardAsync[RespT, ReqT](methodDescriptor: MethodDescriptor[ReqT, RespT], headers: Metadata,
                                         incomingRequest: ReqT, destination: MemberAddress)
                                        (implicit executor: Executor) : ListenableFuture[RespT] = {
    mock.forwardAsync(methodDescriptor, headers, incomingRequest, destination)
  }
}

class ParallelReplicatorSpec extends FlatSpec with BeforeAndAfter {
  "ParallelReplicator" should "forward requests to target nodes in parallel" in {
    implicit val mockExecutor = mock(classOf[Executor])
    val mockReplicator = mock(classOf[ParallelReplicator])
    val replicator = new MockParallelReplicator(2, MemberAddress("host1", 1), mockReplicator)
    val serverCall = mock(classOf[ServerCall[Int, Int]])
    val delegate = mock(classOf[ServerCall.Listener[Int]])
    val headers = new Metadata()
    val destination1 = MemberAddress("host2", 2)
    val destination2 = MemberAddress("host3", 3)

    when(mockReplicator.forwardAsync(any(classOf[MethodDescriptor[Int, Int]]), any(classOf[Metadata]), anyInt(), Matchers.eq(destination1))(any(classOf[Executor])))
      .thenReturn(Futures.immediateFuture(2))
    when(mockReplicator.forwardAsync(any(classOf[MethodDescriptor[Int, Int]]), any(classOf[Metadata]), anyInt(), Matchers.eq(destination2))(any(classOf[Executor])))
      .thenReturn(Futures.immediateFuture(3))

    replicator.replicate[Int, Int](List(destination1, destination2), serverCall, headers, 1, delegate)

    verify(mockReplicator, times(1)).forwardAsync(any(classOf[MethodDescriptor[Int, Int]]), any(classOf[Metadata]), anyInt(), Matchers.eq(destination1))(any(classOf[Executor]))
    verify(mockReplicator, times(1)).forwardAsync(any(classOf[MethodDescriptor[Int, Int]]), any(classOf[Metadata]), anyInt(), Matchers.eq(destination2))(any(classOf[Executor]))

  }

  it should "forward requests to target nodes in parallel & once done, should delegate to local node if it's in the replica node list" in {
    implicit val mockExecutor = mock(classOf[Executor])
    val mockReplicator = mock(classOf[ParallelReplicator])
    val replicator = new MockParallelReplicator(2, MemberAddress("host1", 1), mockReplicator)
    val serverCall = mock(classOf[ServerCall[Int, Int]])
    val delegate = mock(classOf[ServerCall.Listener[Int]])
    val headers = new Metadata()
    val destination1 = MemberAddress("host2", 2)
    val destination2 = MemberAddress("host1", 1)

    when(mockReplicator.forwardAsync(any(classOf[MethodDescriptor[Int, Int]]), any(classOf[Metadata]), anyInt(), Matchers.eq(destination1))(any(classOf[Executor])))
      .thenReturn(Futures.immediateFuture(2))

    replicator.replicate[Int, Int](List(destination1, destination2), serverCall, headers, 1, delegate)

    verify(mockReplicator, times(1)).forwardAsync(any(classOf[MethodDescriptor[Int, Int]]), any(classOf[Metadata]), anyInt(), Matchers.eq(destination1))(any(classOf[Executor]))
    verify(delegate, times(1)).onMessage(Matchers.eq(1))

  }
}
