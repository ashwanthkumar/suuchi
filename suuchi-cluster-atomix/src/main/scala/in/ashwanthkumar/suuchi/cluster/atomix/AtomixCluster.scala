package in.ashwanthkumar.suuchi.cluster.atomix

import java.io.File
import java.time.Duration
import java.util.function.Consumer

import in.ashwanthkumar.suuchi.cluster.{MemberListener, SeedProvider, MemberAddress, Cluster}
import io.atomix.AtomixReplica
import io.atomix.catalyst.transport.Address
import io.atomix.catalyst.transport.netty.NettyTransport
import io.atomix.copycat.server.storage.{Storage, StorageLevel}
import io.atomix.group.{GroupMember, LocalMember}
import org.slf4j.LoggerFactory

import scala.collection.JavaConversions._

/**
 * State object that's stored as metadata associated with the member. We store this node's [[MemberAddress]] information
 * so it's available for others to consume if they want use it for communication at a later point
 *
 * @param address MemberAddress of this node
 */
case class MemberState(address: MemberAddress)

class AtomixCluster(host: String, port: Int, workDir: String, clusterIdentifier: String, listeners: List[MemberListener] = Nil) extends Cluster(listeners) {
  private val log = LoggerFactory.getLogger(classOf[AtomixCluster])

  var atomix = AtomixReplica.builder(new Address(host, port))
    .withTransport(NettyTransport.builder().build())
    .withStorage(
      Storage.builder()
        .withDirectory(new File(workDir, host + "_" + port))
        .withStorageLevel(StorageLevel.DISK)
        .withMinorCompactionInterval(Duration.ofSeconds(30))
        .withMajorCompactionInterval(Duration.ofMinutes(10))
        .withFlushOnCommit()
        .build()
    )
    .build()

  var me: LocalMember = _

  override def start(seedProvider: SeedProvider): AtomixCluster = {
    if (seedProvider.nodes.isEmpty) {
      atomix = atomix.bootstrap().join()
    } else {
      atomix = atomix.join(seedProvider.nodes.map(m => new Address(m.host, m.port))).join()
    }

    val group = atomix.getGroup(clusterIdentifier).join()
    me = group.join(MemberState(MemberAddress(host, port))).join()
    // register a shutdown hook right away
    Runtime.getRuntime.addShutdownHook(new Thread() {
      override def run(): Unit = {
        me.leave()
      }
    })

    group.onJoin(new Consumer[GroupMember] {
      override def accept(t: GroupMember): Unit = {
        val memberState = t.metadata[MemberState]()
        if (memberState.isPresent) {
          onJoin(memberState.get().address)
        } else {
          log.warn("No memberstate associated with the node. Listeners wouldn't be triggered.")
        }
      }
    })

    group.onLeave(new Consumer[GroupMember] {
      override def accept(t: GroupMember): Unit = {
        val memberState = t.metadata[MemberState]()
        if (memberState.isPresent) {
          onLeave(memberState.get().address)
        } else {
          log.warn("No memberstate associated with the node. Listeners wouldn't be triggered.")
        }
      }
    })

    this
  }

  override def nodes: Iterable[MemberAddress] = {
    atomix.getGroup(clusterIdentifier).get()
      .members()
      .map(t => t.metadata[MemberState]())
      .filter(_.isPresent)
      .map(_.get().address)
  }

  override def stop(): Unit = {
    me.leave().join()
  }

  override def whoami: MemberAddress = MemberAddress(host, port)
}