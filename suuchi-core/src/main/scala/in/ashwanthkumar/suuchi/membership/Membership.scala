package in.ashwanthkumar.suuchi.membership

abstract class Membership {
  def bootstrap(bootstrapper: Bootstrapper): Membership

  def start(): Membership

  def stop(): Unit

  def onJoin: MemberAddress => Unit

  def onLeave: MemberAddress => Unit

  def nodes: Iterable[MemberAddress]

  def whoami: MemberAddress
}
