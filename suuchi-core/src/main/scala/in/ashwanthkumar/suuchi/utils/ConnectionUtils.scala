package in.ashwanthkumar.suuchi.utils

trait ConnectionUtils extends Logging {
  //TODO: Add max retry count
  def exponentialBackoffTill(fn: () => Boolean, message: String) = {
    var count = 0
    while (!fn.apply()) {
      val sleepDuration: Long = math.pow(2.0, count).toInt * 100
      log.info(message)
      log.debug(s"Waiting for ${sleepDuration}ms forward channel.")
      Thread.sleep(sleepDuration)
      count = count + 1
    }
  }
}