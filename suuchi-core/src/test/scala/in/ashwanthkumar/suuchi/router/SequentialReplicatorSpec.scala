package in.ashwanthkumar.suuchi.router

import in.ashwanthkumar.suuchi.membership.MemberAddress
import io.grpc.{Metadata, MethodDescriptor, ServerCall, Status}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.mockito.{ArgumentCaptor, Matchers}
import org.scalatest.FlatSpec
import org.scalatest.Matchers.{be, convertToAnyShouldWrapper}

class TestSequentialReplicator(nrReplicas: Int, self: MemberAddress) extends SequentialReplicator(nrReplicas, self) {
  override def forward[RespT, ReqT](methodDescriptor: MethodDescriptor[ReqT, RespT], headers: Metadata, incomingRequest: ReqT, destination: MemberAddress): Any = {}
}

class MockSequentialReplicator(nrReplicas: Int, self: MemberAddress, mock: SequentialReplicator) extends SequentialReplicator(nrReplicas, self) {
  override def forward[RespT, ReqT](methodDescriptor: MethodDescriptor[ReqT, RespT], headers: Metadata, incomingRequest: ReqT, destination: MemberAddress): Any = {
    mock.forward(methodDescriptor, headers, incomingRequest, destination)
  }
}

class SequentialReplicatorSpec extends FlatSpec {
  "SequentialReplicator" should "fail if number of nodes is < expected replicas" in {
    val replicator = new TestSequentialReplicator(3, MemberAddress("host1", 1))
    val serverCall = mock(classOf[ServerCall[Int, Int]])
    val delegate = mock(classOf[ServerCall.Listener[Int]])
    val headers = new Metadata()
    replicator.replicate[Int, Int](List(MemberAddress("host1", 1)), serverCall, headers, 1, delegate)

    val statusCaptor = ArgumentCaptor.forClass(classOf[Status])
    verify(serverCall, times(1)).close(statusCaptor.capture(), Matchers.eq(headers))
    val actualStatus = statusCaptor.getValue
    actualStatus.getDescription should be("We don't have enough nodes to satisfy the replication factor. Not processing this request")
    actualStatus.getCode should be(Status.FAILED_PRECONDITION.getCode)
  }

  it should "fail if no nodes were sent to replicate" in {
    val replicator = new TestSequentialReplicator(0, MemberAddress("host1", 1))
    val serverCall = mock(classOf[ServerCall[Int, Int]])
    val delegate = mock(classOf[ServerCall.Listener[Int]])
    val headers = new Metadata()
    replicator.replicate[Int, Int](Nil, serverCall, headers, 1, delegate)

    val statusCaptor = ArgumentCaptor.forClass(classOf[Status])
    verify(serverCall, times(1)).close(statusCaptor.capture(), Matchers.eq(headers))
    val actualStatus = statusCaptor.getValue
    actualStatus.getDescription should be("This should never happen. No nodes found to place replica")
    actualStatus.getCode should be(Status.INTERNAL.getCode)
  }

  it should "sequentially send forwards to the replicas" in {
    val mockReplicator = mock(classOf[SequentialReplicator])
    val replicator = new MockSequentialReplicator(2, MemberAddress("host1", 1), mockReplicator)
    val serverCall = mock(classOf[ServerCall[Int, Int]])
    val delegate = mock(classOf[ServerCall.Listener[Int]])
    val headers = new Metadata()
    val destination1 = MemberAddress("host2", 2)
    val destination2 = MemberAddress("host3", 3)
    replicator.replicate[Int, Int](List(destination1, destination2), serverCall, headers, 1, delegate)

    verify(mockReplicator, times(1)).forward(any(classOf[MethodDescriptor[Int, Int]]), any(classOf[Metadata]), anyInt(), Matchers.eq(destination1))
    verify(mockReplicator, times(1)).forward(any(classOf[MethodDescriptor[Int, Int]]), any(classOf[Metadata]), anyInt(), Matchers.eq(destination2))
  }

  it should "call delegate.OnMesssage if one of the nodes to replica is self" in {
    val mockReplicator = mock(classOf[SequentialReplicator])
    val replicator = new MockSequentialReplicator(2, MemberAddress("host1", 1), mockReplicator)
    val serverCall = mock(classOf[ServerCall[Int, Int]])
    val delegate = mock(classOf[ServerCall.Listener[Int]])
    val headers = new Metadata()
    val destination1 = MemberAddress("host1", 1)
    val destination2 = MemberAddress("host2", 2)
    replicator.replicate[Int, Int](List(destination1, destination2), serverCall, headers, 1, delegate)

    verify(mockReplicator, times(0)).forward(any(classOf[MethodDescriptor[Int, Int]]), any(classOf[Metadata]), anyInt(), Matchers.eq(destination1))
    verify(mockReplicator, times(1)).forward(any(classOf[MethodDescriptor[Int, Int]]), any(classOf[Metadata]), anyInt(), Matchers.eq(destination2))
    verify(delegate, times(1)).onMessage(Matchers.eq(1))
  }
}
