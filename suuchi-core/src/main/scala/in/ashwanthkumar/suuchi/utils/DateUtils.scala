package in.ashwanthkumar.suuchi.utils

import org.joda.time.DateTime

trait DateUtils {
  def now = DateTime.now().getMillis
}
