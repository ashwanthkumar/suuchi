package in.ashwanthkumar.suuchi.models

trait TriggerType
object TriggerTypes {
  case object CHANGED extends TriggerType
  case object NOT_CHANGED extends TriggerType
}

/**
 * Triggers are events that're triggered whenever there's a change
 */
case class Trigger(key: Array[Byte], before: Array[Byte], after: Array[Byte], `type`: TriggerType)
