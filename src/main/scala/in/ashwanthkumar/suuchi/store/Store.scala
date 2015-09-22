package in.ashwanthkumar.suuchi.store

trait StoreConfig

trait Store[K, V] {
  @throws[Exception]
  def init(config: StoreConfig)

  def get(key: K): V
  def bulkGet(keys: List[K]): Map[K, V]
  def put(key: K, value: V, timestamp: Long): Unit
}
