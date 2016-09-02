package in.ashwanthkumar.suuchi.cluster.fsm

import in.ashwanthkumar.suuchi.cluster.Logging
import org.apache.helix.NotificationContext
import org.apache.helix.model.Message
import org.slf4j.LoggerFactory

/**
 * FSM of the MasterSlave app
 */
trait SucchiFSM {

  def fromOfflineToSlave(message: Message, context: NotificationContext) {}

  def fromSlaveToMaster(message: Message, context: NotificationContext) {}

  def fromMasterToSlave(message: Message, context: NotificationContext) {}

  def fromSlaveToOffline(message: Message, context: NotificationContext) {}

  def fromOfflineToDropped(message: Message, context: NotificationContext) {}
}


class DummyFSM extends SucchiFSM with Logging {

  override def fromOfflineToSlave(message: Message, context: NotificationContext): Unit = {
    log.info(s"Moving from Offline to Slave via $message and $context")
  }
  override def fromSlaveToMaster(message: Message, context: NotificationContext): Unit = {
    log.info(s"Moving from Slave to Master via $message and $context")
  }
  override def fromMasterToSlave(message: Message, context: NotificationContext): Unit = {
    log.info(s"Moving from Master to Slave via $message and $context")
  }
  override def fromSlaveToOffline(message: Message, context: NotificationContext): Unit = {
    log.info(s"Moving from Slave to Offline via $message and $context")
  }
  override def fromOfflineToDropped(message: Message, context: NotificationContext): Unit = {
    log.info(s"Moving from Offline to Dropped via $message and $context")
  }
}