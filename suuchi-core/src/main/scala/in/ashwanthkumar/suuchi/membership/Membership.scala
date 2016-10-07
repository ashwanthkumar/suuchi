package in.ashwanthkumar.suuchi.membership

abstract class Membership {
  def bootstrap(bootstrapper: Bootstrapper): Membership

  def start(): Membership

  def stop(): Unit

  def onJoin: Member => Unit

  def onLeave: Member => Unit

  def nodes: Iterable[Member]

  def whoami: Member
}
