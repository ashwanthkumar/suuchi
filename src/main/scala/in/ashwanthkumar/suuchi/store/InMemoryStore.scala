package in.ashwanthkumar.suuchi.store

import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap

import org.slf4j.LoggerFactory

class InMemoryStore extends Store {
  private val log = LoggerFactory.getLogger(getClass)
  private val store = new ConcurrentHashMap[ByteBuffer, Array[Byte]]()

  override def put(key: Array[Byte], value: Array[Byte]): Boolean = {
    log.info(s"Put with key=${new String(key)}, value=${new String(value)}")
    store.put(ByteBuffer.wrap(key), value)
    true
  }
  override def get(key: Array[Byte]): Option[Array[Byte]] = {
    log.info(s"Get with key=${new String(key)}")
    val value = Option(store.get(ByteBuffer.wrap(key)))
    log.info(s"GetResult for key=${new String(key)}, value=${value.map(b => new String(b))}")
    value
  }
}
