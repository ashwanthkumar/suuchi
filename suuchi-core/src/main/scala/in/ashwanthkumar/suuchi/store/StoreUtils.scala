package in.ashwanthkumar.suuchi.store

trait CloseableIterator[T] extends Iterator[T] with AutoCloseable

object StoreUtils {

  def scan[T](scanner: Scanner[T]): CloseableIterator[T] = {
    scanner.prepare()
    val it = scanner.scan()
    new CloseableIterator[T] {
      var closed = false

      override def hasNext: Boolean = {
        val result = !closed && it.hasNext
        if(!closed && !result) close()
        result
      }
      override def next(): T = it.next()
      override def close(): Unit = {
        scanner.close()
        closed = true
      }
    }
  }

  def scan[T](prefix: Array[Byte], scanner: Scanner[T]): CloseableIterator[T] = {
    scanner.prepare()
    val it = scanner.scan(prefix)
    new CloseableIterator[T] {
      var closed = false

      override def hasNext: Boolean = {
        val result = !closed && it.hasNext
        if(!closed && !result) close()
        result
      }
      override def next(): T = it.next()
      override def close(): Unit = {
        scanner.close()
        closed = true
      }
    }
  }

}
