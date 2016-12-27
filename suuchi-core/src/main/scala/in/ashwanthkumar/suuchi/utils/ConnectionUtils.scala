package in.ashwanthkumar.suuchi.utils

trait ConnectionUtils extends Logging {
  def exponentialBackoffTill(fn: () => Boolean, logString: String) = {
    var count = 0
    while (!fn.apply()) {
      val sleepDuration: Long = math.pow(2.0, count).toInt * 100
      log.debug(s"Waiting for ${sleepDuration}ms forward channel. Log message: $logString")
      Thread.sleep(sleepDuration)
      count = count + 1
    }
  }
}