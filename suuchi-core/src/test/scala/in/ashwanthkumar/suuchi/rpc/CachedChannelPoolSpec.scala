package in.ashwanthkumar.suuchi.rpc

import java.util.concurrent.{TimeUnit, ConcurrentHashMap}

import in.ashwanthkumar.suuchi.membership.MemberAddress
import io.grpc.{Channel, ManagedChannelBuilder, ManagedChannel}
import org.scalatest.FlatSpec
import org.scalatest.Matchers.{convertToAnyShouldWrapper, be, have, size}
import org.mockito.Mockito._
import org.mockito.Matchers._

class MockCachedPool(map: ConcurrentHashMap[String, ManagedChannel], mockBuilder: ManagedChannelBuilder[_ <: ManagedChannelBuilder[_]]) extends CachedChannelPool(map) {
  override private[rpc] def builderFrom(key: String): ManagedChannelBuilder[_] = mockBuilder
}

class CachedChannelPoolSpec extends FlatSpec {
  "CachedChannelPool" should "build a new channel if no cache is available" in {
    val mockChannel = mock(classOf[ManagedChannel])
    val builder = mock(classOf[ManagedChannelBuilder[_ <: ManagedChannelBuilder[_]]])
    when(builder.build()).thenReturn(mockChannel)

    val map = new ConcurrentHashMap[String, ManagedChannel]()
    val pool = new MockCachedPool(map, builder)
    map should have size 0
    val channel = pool.get(MemberAddress("host1", 1))
    map should have size 1
    channel.shutdown()

    verify(mockChannel, times(1)).shutdown()
    verify[ManagedChannelBuilder[_]](builder, times(1)).build()
  }

  it should "build a new channel using plainText if insecure=true" in {
    val mockChannel = mock(classOf[ManagedChannel])
    val builder = mock(classOf[ManagedChannelBuilder[_ <: ManagedChannelBuilder[_]]])
    when(builder.build()).thenReturn(mockChannel)

    val map = new ConcurrentHashMap[String, ManagedChannel]()
    val pool = new MockCachedPool(map, builder)
    map should have size 0
    val channel = pool.get(MemberAddress("host1", 1), insecure = true)
    map should have size 1
    channel.shutdown()

    verify[ManagedChannelBuilder[_]](builder, times(1)).usePlaintext(true)
    verify[ManagedChannelBuilder[_]](builder, times(1)).build()
    verify(mockChannel, times(1)).shutdown()
  }

  it should "return the same channel if it's already cached" in {
    val mockChannel = mock(classOf[ManagedChannel])
    val builder = mock(classOf[ManagedChannelBuilder[_ <: ManagedChannelBuilder[_]]])
    when(builder.build()).thenReturn(mockChannel)

    val map = new ConcurrentHashMap[String, ManagedChannel]()
    val pool = new MockCachedPool(map, builder)
    map should have size 0
    val channel = pool.get(MemberAddress("host1", 1))
    map should have size 1
    val anotherChannel = pool.get(MemberAddress("host1", 1))
    map should have size 1

    verify[ManagedChannelBuilder[_]](builder, times(1)).build()
  }
}
