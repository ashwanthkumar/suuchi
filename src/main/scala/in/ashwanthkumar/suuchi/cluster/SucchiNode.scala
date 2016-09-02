package in.ashwanthkumar.suuchi.cluster

import in.ashwanthkumar.suuchi.cluster.fsm.{DummyFSM, SucchiFSM}
import org.apache.helix.api.id.PartitionId
import org.apache.helix.api.{StateTransitionHandlerFactory, TransitionHandler}
import org.apache.helix.model.Message
import org.apache.helix.participant.statemachine.{StateModelInfo, Transition}
import org.apache.helix.{HelixManager, NotificationContext}

class SucchiNode extends Node {
  override def stateTransitionFactory(manager: HelixManager): StateTransitionHandlerFactory[_ <: TransitionHandler] = new StateTransitionHandlerFactory[SucchiStateModelWrapper] {
    override def createStateTransitionHandler(partitionId: PartitionId): SucchiStateModelWrapper = {
      log.info(s"Creating State Transition handler for $partitionId")
      new SucchiStateModelWrapper(new DummyFSM)
    }
  }
  /**
   * Add any custom listeners for the node if needed via the manager
   * @param manager
   */
  override def preStart(manager: HelixManager, clusterName: String): Unit = {
    super.preStart(manager, clusterName)

    // TODO - remove this - this should be plugged (from sub classes)
    val resources = admin.getResourcesInCluster(clusterName)
    val DEFAULT_RESOURCE = "resource"
    if (!resources.contains(DEFAULT_RESOURCE)) {
      admin.addResource(clusterName, DEFAULT_RESOURCE, 3, Node.DEFAULT_STATE_MODEL)
    }
    admin.rebalance(clusterName, DEFAULT_RESOURCE, 3)
  }
}

@StateModelInfo(initialState = "OFFLINE", states = Array("OFFLINE", "MASTER", "SLAVE"))
class SucchiStateModelWrapper(stateMachineModel: SucchiFSM) extends TransitionHandler {

  @Transition(from = "OFFLINE", to = "SLAVE")
  def fromOfflineToSlave(message: Message, context: NotificationContext): Unit = {
    stateMachineModel.fromOfflineToSlave(message, context)
  }

  @Transition(from = "SLAVE", to = "MASTER")
  def fromSlaveToMaster(message: Message, context: NotificationContext): Unit = {
    stateMachineModel.fromSlaveToMaster(message, context)
  }

  @Transition(from = "MASTER", to = "SLAVE")
  def fromMasterToSlave(message: Message, context: NotificationContext): Unit = {
    stateMachineModel.fromMasterToSlave(message, context)
  }

  @Transition(from = "SLAVE", to = "OFFLINE")
  def fromSlaveToOffline(message: Message, context: NotificationContext): Unit = {
    stateMachineModel.fromSlaveToOffline(message, context)
  }

  @Transition(from = "OFFLINE", to = "DROPPED")
  def fromOfflineToDropped(message: Message, context: NotificationContext): Unit = {
    stateMachineModel.fromOfflineToDropped(message, context)
  }
}


