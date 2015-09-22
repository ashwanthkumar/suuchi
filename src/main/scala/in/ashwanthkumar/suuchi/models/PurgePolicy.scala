package in.ashwanthkumar.suuchi.models

import in.ashwanthkumar.suuchi.util.Clock
import org.joda.time.{DateTime, Days}

trait PurgePolicy {
  def shouldPurge(version: Batch): Boolean
}

case class NumberOfDaysBasedPurgePolicy(durationInDays: Int, clock: Clock = Clock) extends PurgePolicy {
  override def shouldPurge(version: Batch): Boolean = Days.daysBetween(clock.now, new DateTime(version.loadedAt)).getDays > durationInDays
}

case object NoPurgePolicy extends PurgePolicy {
  override def shouldPurge(version: Batch): Boolean = false
}