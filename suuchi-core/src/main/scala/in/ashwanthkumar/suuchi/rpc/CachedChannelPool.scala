package in.ashwanthkumar.suuchi.rpc

import java.util.concurrent.ConcurrentHashMap

import in.ashwanthkumar.suuchi.membership.MemberAddress
import io.grpc.{ManagedChannel, ManagedChannelBuilder}

import scala.language.existentials

class CachedChannelPool(map: ConcurrentHashMap[String, ManagedChannel]) {
  def get(node: MemberAddress, insecure: Boolean = false): ManagedChannel = {
    val target = node.toExternalForm
    if (map.containsKey(target)) {
      map.get(target)
    } else {
      val builder = builderFrom(target)
      if (insecure) {
        builder.usePlaintext(true)
      }
      val channel = builder.build()
      map.put(target, channel)
      channel
    }
  }

  private[rpc] def builderFrom(key: String): ManagedChannelBuilder[_] = {
    ManagedChannelBuilder.forTarget(key)
  }
}

object CachedChannelPool {
  private val _default = new CachedChannelPool(new ConcurrentHashMap[String, ManagedChannel]())

  def apply(): CachedChannelPool = _default
}

