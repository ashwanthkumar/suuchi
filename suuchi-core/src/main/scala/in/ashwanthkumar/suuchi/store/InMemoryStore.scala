package in.ashwanthkumar.suuchi.store

import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap

import org.slf4j.LoggerFactory

class InMemoryStore extends Store {
  private val log = LoggerFactory.getLogger(getClass)
  private val store = new ConcurrentHashMap[ByteBuffer, Array[Byte]]()

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
}
