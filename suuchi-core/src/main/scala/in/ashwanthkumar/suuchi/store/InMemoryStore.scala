package in.ashwanthkumar.suuchi.store

import java.nio.ByteBuffer
import java.util.{Arrays => JArrays}
import java.util.concurrent.ConcurrentSkipListMap

import org.slf4j.LoggerFactory

import scala.collection.JavaConversions._

class InMemoryStore extends Store {
  private val log = LoggerFactory.getLogger(getClass)
  private val store = new ConcurrentSkipListMap[ByteBuffer, Array[Byte]]()

  override def put(key: Array[Byte], value: Array[Byte]): Boolean = {
    log.trace(s"Put with key=${new String(key)}, value=${new String(value)}")
    store.put(ByteBuffer.wrap(key), value)
    true
  }

  override def get(key: Array[Byte]): Option[Array[Byte]] = {
    log.trace(s"Get with key=${new String(key)}")
    val value = Option(store.get(ByteBuffer.wrap(key)))
    log.trace(s"GetResult for key=${new String(key)}, value=${value.map(b => new String(b))}")
    value
  }

  override def remove(key: Array[Byte]): Boolean = {
    log.trace(s"Remove for key=${new String(key)}")
    store.remove(ByteBuffer.wrap(key))
    true
  }

  override def scan(): Iterator[KV] = {
    store.entrySet().map(kv => KV(kv.getKey.array(), kv.getValue)).iterator
  }

  private def hasPrefix(key: Array[Byte], prefix: Array[Byte]) = {
    key.length >= prefix.length && JArrays.equals(key.take(prefix.length), prefix)
  }

  override def scan(prefix: Array[Byte]): Iterator[KV] = {
    store.tailMap(ByteBuffer.wrap(prefix))
      .takeWhile{case (k, v) => hasPrefix(k.array(), prefix)}
      .map{case (k, v) => KV(k.array(), v)}
      .iterator
  }
}
