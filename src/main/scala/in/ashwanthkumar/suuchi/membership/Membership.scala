package in.ashwanthkumar.suuchi.membership

import java.io.File
import java.time.Duration
import java.util
import java.util.function.Consumer

import io.atomix.AtomixReplica
import io.atomix.catalyst.transport
import io.atomix.catalyst.transport.Address
import io.atomix.copycat.server.storage.{StorageLevel, Storage}
import io.atomix.group.{DistributedGroup, LocalMember, GroupMember}

import scala.collection.JavaConversions._

case class MemberAddress(host: String, port: Int)
case class Member(id: String)

trait Bootstrapper {
  /**
   * Returns a list of host
   * @return
   */
  def nodes: List[MemberAddress]
}


abstract class Membership(host: String, port: Int) {
  def bootstrap(bootstrapper: Bootstrapper): Membership

  def start(): Membership

  def onJoin: Member => Unit

  def onLeave: Member => Unit
}

class AtomixMembership(host: String, port: Int, workDir: String, clusterIdentifier: String) extends Membership(host, port) {

  val replica = AtomixReplica.builder(new Address(host, port))
    .withStorage(
      Storage.builder()
        .withDirectory(new File(workDir, host + "_" + port))
        .withStorageLevel(StorageLevel.DISK)
        .withMinorCompactionInterval(Duration.ofSeconds(30))
        .withMajorCompactionInterval(Duration.ofMinutes(10))
        .build()
    )
    .build()

  var group: DistributedGroup = _
  var me: LocalMember = _

  override def bootstrap(bootstrapper: Bootstrapper): AtomixMembership = {
    replica.bootstrap(bootstrapper.nodes.map(m => new Address(m.host, m.port))).join()
    this
  }

  override def start(): AtomixMembership = {
    group = replica.getGroup(clusterIdentifier).join()
    me = group.join().join()
    replica.getValue(s"nodes/${me.id()}").join().getAndSet(s"$host:$port").join()
    // register a shutdown hook right away
    Runtime.getRuntime.addShutdownHook(new Thread() {
      override def run(): Unit = {
        me.leave()
      }
    })

    group.onJoin(new Consumer[GroupMember] {
      override def accept(t: GroupMember): Unit = onJoin.apply(Member(t.id()))
    })

    group.onLeave(new Consumer[GroupMember] {
      override def accept(t: GroupMember): Unit = onLeave.apply(Member(t.id()))
    })

    this
  }

  override def onJoin: (Member) => Unit = (m: Member) => {
    val hostPort = replica.getValue[String](s"nodes/${m.id}").join().get().join()
    println(s"$m ($hostPort) has joined")
    println(s"Total Members - ${group.members().mkString("\n")}")
  }
  override def onLeave: (Member) => Unit = (m: Member) => {
    println(s"$m has left")
    println(s"Total Members - ${group.members().mkString("\n")}")
  }
}

case class InMemoryBootstrapper(override val nodes: List[MemberAddress]) extends Bootstrapper

object TestMembership extends App {
  val port = args(0).toInt

  val bootstrapper = {
    if (port != 50001) {
      InMemoryBootstrapper(List(MemberAddress("localhost", 50001)))
    } else {
      InMemoryBootstrapper(List())
    }
  }

  val membership = new AtomixMembership("localhost", port, "/tmp/suuchi-raft", "succhi-test-group")
    .bootstrap(bootstrapper)
    .start()

}