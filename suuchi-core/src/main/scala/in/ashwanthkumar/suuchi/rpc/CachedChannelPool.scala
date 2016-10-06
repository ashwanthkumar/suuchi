package in.ashwanthkumar.suuchi.rpc

import java.util.concurrent.ConcurrentHashMap

import in.ashwanthkumar.suuchi.membership.MemberAddress
import io.grpc.{ManagedChannel, ManagedChannelBuilder}

import scala.language.existentials

class CachedChannelPool(map: ConcurrentHashMap[String, ManagedChannel]) {
  def get(node: MemberAddress, insecure: Boolean = false): ManagedChannel = {
    val key = node.toExternalForm
    if (map.containsKey(key)) {
      map.get(key)
    } else {
      val builder = ManagedChannelBuilder.forTarget(key)
      if (insecure) {
        builder.usePlaintext(true)
      }
      val channel = builder.build()
      map.put(key, channel)
      channel
    }
  }
}

object CachedChannelPool {
  private val _default = new CachedChannelPool(new ConcurrentHashMap[String, ManagedChannel]())

  def apply(): CachedChannelPool = _default
}

