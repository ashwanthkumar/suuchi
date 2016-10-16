package in.ashwanthkumar.suuchi.store.rocksdb

import org.rocksdb.Options

case class RocksDbConfiguration(location: String,
                                batchSize: Int,
                                readOnly: Boolean,
                                memTableSize: Int,
                                numFilesAtBase: Int,
                                maxBytesForBaseLevel: Long,
                                maxWriteBufferNumber: Int,
                                maxBackgroundCompactions: Int,
                                baseOptions: Options) {
  val perFileSizeAtBase = maxBytesForBaseLevel / numFilesAtBase
  def toOptions = {
    baseOptions
      .setMaxBytesForLevelBase(maxBytesForBaseLevel)
      .setTargetFileSizeBase(perFileSizeAtBase)
      .setWriteBufferSize(memTableSize)
      .setMaxWriteBufferNumber(maxWriteBufferNumber)
      .setMaxBackgroundCompactions(maxBackgroundCompactions)
  }
}

object RocksDbConfiguration {
  val BATCH_SIZE = 100000
  val DESIRED_NUM_FILES_AT_BASE_LEVEL = 10
  val MEMTABLE_SIZE = 128 * 1024 * 1024
  val MAX_BYTES_FOR_BASE_LEVEL = 4l * 1024 * 1024 * 1024
  val MAX_WRITE_BUFFER_NUMBER = 2
  val MAX_BG_COMPACTIONS = 2

  def apply(location: String): RocksDbConfiguration = apply(location, BATCH_SIZE, false, MEMTABLE_SIZE,
    DESIRED_NUM_FILES_AT_BASE_LEVEL, MAX_BYTES_FOR_BASE_LEVEL, MAX_WRITE_BUFFER_NUMBER, MAX_BG_COMPACTIONS,
    new Options().setCreateIfMissing(true))

  def apply(location: String, options: Options): RocksDbConfiguration = new RocksDbConfiguration(location, BATCH_SIZE,
    false, MEMTABLE_SIZE, DESIRED_NUM_FILES_AT_BASE_LEVEL, MAX_BYTES_FOR_BASE_LEVEL, MAX_WRITE_BUFFER_NUMBER,
    MAX_BG_COMPACTIONS, options)
}
