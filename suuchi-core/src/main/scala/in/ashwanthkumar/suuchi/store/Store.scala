package in.ashwanthkumar.suuchi.store

trait ReadStore {
  def get(key: Array[Byte]): Option[Array[Byte]]
}

trait WriteStore {
  def put(key: Array[Byte], value: Array[Byte]): Boolean
}

trait DeleteStore {
  def remove(key: Array[Byte]) : Boolean
}

trait Store extends ReadStore with WriteStore with DeleteStore
