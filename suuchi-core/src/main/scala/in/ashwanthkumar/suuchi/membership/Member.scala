package in.ashwanthkumar.suuchi.membership

case class Member(id: String)

case class MemberAddress(host: String, port: Int) {
  def toExternalForm = s"$host:$port"
}

object MemberAddress {
  /**
   * Constructs a MemberAddress from host:port string format
   *
   * @param hostPort  Host:Port format of a node address
   * @return MemberAddress
   */
  def apply(hostPort: String): MemberAddress = {
    val parts = hostPort.split(":")
    MemberAddress(parts(0), parts(1).toInt)
  }
}
