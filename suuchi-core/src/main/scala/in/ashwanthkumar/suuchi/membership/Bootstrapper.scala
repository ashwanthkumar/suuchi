package in.ashwanthkumar.suuchi.membership

trait Bootstrapper {
  /**
   * Returns a list of host
   * @return
   */
  def nodes: List[MemberAddress]
}

case class InMemoryBootstrapper(override val nodes: List[MemberAddress]) extends Bootstrapper
