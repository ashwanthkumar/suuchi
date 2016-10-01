package in.ashwanthkumar.suuchi.store

import java.util.concurrent.ConcurrentHashMap

import com.google.protobuf.ByteString

class InMemoryStore extends Store {
  private val store = new ConcurrentHashMap[ByteString, ByteString]()

  override def put(key: Array[Byte], value: Array[Byte]): Boolean = {
    store.put(ByteString.copyFrom(key), ByteString.copyFrom(value))
    true
  }
  override def get(key: Array[Byte]): Option[Array[Byte]] = Option(store.get(ByteString.copyFrom(key))).map(_.toByteArray)
}
