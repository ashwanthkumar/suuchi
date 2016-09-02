package in.ashwanthkumar.suuchi.cluster

import org.apache.helix.api.id.StateModelDefId
import org.apache.helix.api.{StateTransitionHandlerFactory, TransitionHandler}
import org.apache.helix.controller.HelixControllerMain
import org.apache.helix.manager.zk.{ZKHelixAdmin, ZNRecordSerializer, ZkClient}
import org.apache.helix.model.{InstanceConfig, StateModelDefinition}
import org.apache.helix.tools.StateModelConfigGenerator
import org.apache.helix._

abstract class Node extends Logging {

  var admin: ZKHelixAdmin = _
  var manager: HelixManager = _
  var controller: HelixManager = _

  /**
   * Start the Node and join the existing (or new) cluster
   * @param zkAddr
   * @param clusterName
   * @param hostname
   * @param port
   */
  final def start(zkAddr: String, clusterName: String, hostname: String, port: Int): Unit = {
    val serverId: String = s"${hostname}_$port"
    log.info("Initializing the HelixAdmin and HelixManager")
    initIfNotAlready(zkAddr, clusterName, serverId)
    // add cluster to ZK if not already
    log.info(s"Adding the cluster $clusterName via HelixAdmin")
    admin.addCluster(clusterName, false) // don't re-create if we already exist

    // add state model definition for the cluster
    log.info(s"Adding the cluster's StateModelDef via HelixAdmin (if not found)")
    val stateModelDef = admin.getStateModelDef(clusterName, Node.DEFAULT_STATE_MODEL)
    if (stateModelDef == null) {
      admin.addStateModelDef(clusterName, Node.DEFAULT_STATE_MODEL, new StateModelDefinition(StateModelConfigGenerator.generateConfigForMasterSlave))
    }
    val config = new InstanceConfig(serverId)
    config.setHostName(hostname)
    config.setPort(port.toString)
    config.setInstanceEnabled(true)
    log.info(s"Adding the instance $config for $clusterName via HelixAdmin")
    val instances = admin.getInstancesInCluster(clusterName)
    if (instances.contains(serverId)) {
      log.info(s"Instance with $config already found, dropping it")
      admin.dropInstance(clusterName, admin.getInstanceConfig(clusterName, serverId))
    }
    admin.addInstance(clusterName, config) // register to the ZK if not already

    val stateMachine = manager.getStateMachineEngine
    stateMachine.registerStateModelFactory(StateModelDefId.from(Node.DEFAULT_STATE_MODEL), stateTransitionFactory(manager))
    log.info("Running preStart hook on the node")
    preStart(manager, clusterName)
    log.info("Finished running preStart hook on the node")
    log.info("Connecting on HelixManager")
    manager.connect()

    Runtime.getRuntime.addShutdownHook(new Thread() {
      override def run() {
        log.info("Shutting down server:" + serverId)
        manager.disconnect()
      }
    })

    // Start Controller with every node
    log.info("Starting a controller for the cluster in the node")
    controller = HelixControllerMain.startHelixController(zkAddr, clusterName, "controller", HelixControllerMain.STANDALONE)
    printClusterStatus(controller)

    log.info("System is fully functional and running")
    Thread.currentThread().join()
  }

  def printClusterStatus(controller: HelixManager): Unit = {
    println("CLUSTER STATUS")
    val helixDataAccessor: HelixDataAccessor = controller.getHelixDataAccessor
    val keyBuilder: PropertyKey.Builder = helixDataAccessor.keyBuilder
    println("External View \n" + helixDataAccessor.getProperty(keyBuilder.externalView("repository")))
  }
  /**
   * Add any custom listeners for the node if needed via the manager
   * @param manager
   */
  def preStart(manager: HelixManager, clusterName: String) = {}

  def stateTransitionFactory(manager: HelixManager): StateTransitionHandlerFactory[_ <: TransitionHandler]

  /**
   * Stop the node and disconnect from the cluster
   */
  def stop(): Unit = {
    if (admin != null) {
      admin.close()
    }
    if (manager != null) {
      manager.disconnect()
    }
  }

  private def initIfNotAlready(zkAddr: String, clusterName: String, serverId: String): Unit = {
    if (admin == null || manager == null) {
      stop()
      val zkClient = new ZkClient(zkAddr, ZkClient.DEFAULT_SESSION_TIMEOUT, ZkClient.DEFAULT_CONNECTION_TIMEOUT, new ZNRecordSerializer)
      admin = new ZKHelixAdmin(zkClient)
      manager = HelixManagerFactory.getZKHelixManager(clusterName, serverId, InstanceType.PARTICIPANT, zkAddr)
      log.info("Initialized the HelixAdmin and HelixManager")
    }

  }
}

object Node {
  val DEFAULT_STATE_MODEL = "MasterSlave"
}