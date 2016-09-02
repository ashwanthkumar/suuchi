package in.ashwanthkumar.suuchi.cluster

object Succhi extends App {

  val port = args(0).toInt
  val node = new SucchiNode()
  node.start(
    zkAddr = "localhost:2199",
    clusterName = "testCluster",
    hostname = "localhost",
    port = port)

}
