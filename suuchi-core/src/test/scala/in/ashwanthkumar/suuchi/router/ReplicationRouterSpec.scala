package in.ashwanthkumar.suuchi.router

import in.ashwanthkumar.suuchi.membership.MemberAddress
import in.ashwanthkumar.suuchi.rpc.CachedChannelPool
import io.grpc.ServerCall.Listener
import io.grpc.{Metadata, MethodDescriptor, ServerCall, ServerCallHandler}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.FlatSpec

object NoReplicatorFactory extends ReplicatorFactory {
  override def create[ReqT](config: ReplicatorConfig, cachedChannelPool: CachedChannelPool): Replicator[ReqT] = new Replicator[ReqT](config) {
    override def doReplication[RespT](eligibleNodes: scala.List[MemberAddress], serverCall: ServerCall[ReqT, RespT], headers: Metadata, incomingRequest: ReqT, delegate: ServerCall.Listener[ReqT]): Unit = {}
  }
}

class NoReplicator(nrOfReplicas: Int, self: MemberAddress) extends ReplicationRouter(nrOfReplicas, self, NoReplicatorFactory) {}

class MockReplicatorFactory(mock: Replicator[_]) extends ReplicatorFactory {
  override def create[ReqT](config: ReplicatorConfig, cachedChannelPool: CachedChannelPool): Replicator[ReqT] = mock.asInstanceOf[Replicator[ReqT]]
}

class MockReplicator(nrOfReplicas: Int, self: MemberAddress, mock: Replicator[Int]) extends ReplicationRouter(nrOfReplicas, self, new MockReplicatorFactory(mock)) {}

class ReplicationRouterSpec extends FlatSpec {
  "ReplicationRouter" should "delegate the message to the local node if it's a REPLICATION_REQUEST" in {
    val whoami = MemberAddress("host1", 1)
    val replicator = new NoReplicator(1, whoami)

    setupAndVerify { (serverCall: ServerCall[Int, Int], delegate: ServerCall.Listener[Int], next: ServerCallHandler[Int, Int]) =>
      when(next.startCall(any(classOf[ServerCall[Int, Int]]), any(classOf[Metadata]))).thenReturn(delegate)
      val headers = new Metadata()
      headers.put(Headers.REPLICATION_REQUEST_KEY, whoami.toString)

      val listener = replicator.interceptCall(serverCall, headers, next)
      listener.onReady()
      listener.onMessage(1)
      listener.onHalfClose()
      listener.onComplete()
      listener.onCancel()

      verify(delegate, times(1)).onReady()
      verify(delegate, times(1)).onMessage(1)
    }
  }

  it should "not do anything if no required headers are present" in {
    val whoami = MemberAddress("host1", 1)
    val replicator = new NoReplicator(1, whoami)

    setupAndVerify { (serverCall: ServerCall[Int, Int], delegate: ServerCall.Listener[Int], next: ServerCallHandler[Int, Int]) =>
      val headers = new Metadata()
      val listener = replicator.interceptCall(serverCall, headers, next)
      listener.onReady()
      listener.onMessage(1)
      listener.onHalfClose()
      listener.onComplete()
      listener.onCancel()

      verify(delegate, times(1)).onReady()
      verify(delegate, times(0)).onMessage(1)
    }
  }

  it should "replicate the request as per replication strategy" in {
    val whoami = MemberAddress("host1", 1)
    val mockReplicator = mock(classOf[Replicator[Int]])
    val replicator = new MockReplicator(1, whoami, mockReplicator)

    setupAndVerify { (serverCall: ServerCall[Int, Int], delegate: ServerCall.Listener[Int], next: ServerCallHandler[Int, Int]) =>
      val headers = new Metadata()
      headers.put(Headers.ELIGIBLE_NODES_KEY, List(whoami))
      val listener = replicator.interceptCall(serverCall, headers, next)
      listener.onReady()
      listener.onMessage(1)
      listener.onHalfClose()
      listener.onComplete()
      listener.onCancel()

      verify(delegate, times(1)).onReady()
      verify(delegate, times(0)).onMessage(1)
      verify(mockReplicator, times(1)).replicate[Int](any(classOf[List[MemberAddress]]), anyInt(), any(classOf[ServerCall[Int, Int]]), any(classOf[ServerCall.Listener[Int]]))
    }
  }

  def setupAndVerify(verify: (ServerCall[Int, Int], ServerCall.Listener[Int], ServerCallHandler[Int, Int]) => Unit): Unit = {
    val serverCall = mock(classOf[ServerCall[Int, Int]])
    val serverMethodDesc = mock(classOf[MethodDescriptor[Int, Int]])
    when(serverCall.getMethodDescriptor).thenReturn(serverMethodDesc)
    when(serverMethodDesc.getFullMethodName).thenReturn("TestService/Test")

    val delegate = mock(classOf[Listener[Int]])
    val next = mock(classOf[ServerCallHandler[Int, Int]])
    when(next.startCall(any(classOf[ServerCall[Int, Int]]), any(classOf[Metadata]))).thenReturn(delegate)

    verify(serverCall, delegate, next)
  }
}
