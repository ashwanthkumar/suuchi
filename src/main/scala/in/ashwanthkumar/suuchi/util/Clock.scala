package in.ashwanthkumar.suuchi.util

import org.joda.time.DateTime

trait Clock {
  def now = DateTime.now()
  def today = now.withTimeAtStartOfDay()
  def yesterday = today.minusDays(1)
  def tomorrow = today.plusDays(1)
}

object Clock extends Clock

case class FakeClock(override val now: DateTime) extends Clock