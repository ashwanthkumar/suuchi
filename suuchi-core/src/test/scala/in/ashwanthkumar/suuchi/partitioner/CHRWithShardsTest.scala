package in.ashwanthkumar.suuchi.partitioner

import in.ashwanthkumar.suuchi.cluster.MemberAddress
import in.ashwanthkumar.suuchi.utils.DistinctMatcher
import org.scalatest.FlatSpec
import org.scalatest.Matchers._

import scala.collection.JavaConversions._

class CHRWithShardsTest extends FlatSpec {

  val distinct = new DistinctMatcher[MemberAddress]

  def printRing(ring: CHRWithShards)= ring.shards.toList.sortBy(_.id).foreach(s => println(s"Id: ${s.id} Key: ${s.key} Primary: ${s.primaryPartition.node.toExternalForm} Followers: ${s.followers.map(_.node.toExternalForm)}"))

  "CHRWithShards" should "pin nodes into the ring accounting for shards" in {
    val nodes = List(MemberAddress("host1", 1), MemberAddress("host2", 2), MemberAddress("host3", 3), MemberAddress("host4", 4), MemberAddress("host5", 5))
    val ring = CHRWithShards(nodes, primaryPartitionsPerNode = 2, replicationFactor = 3)

    ring.shards should have size 10
//    printRing(ring)
  }

  it should "not a node as a follower of itself" in {
    val nodes = List(MemberAddress("host1", 1), MemberAddress("host2", 2), MemberAddress("host3", 3))
    val ring = CHRWithShards(nodes, 1, 3)

    val shards = ring.shards.toList
    shards.foreach{ shard =>
      shard.followers should not contain shard.primaryPartition.node
    }
  }

  it should "have shards with distinct number nodes to replicate data" in {
    val nodes = List(MemberAddress("host1", 1), MemberAddress("host2", 2), MemberAddress("host3", 3))
    val ring = CHRWithShards(nodes, 1, 3)

    val shards = ring.shards.toList

    shards.foreach{shard =>
      shard.getNodes shouldBe distinct
    }
  }
}
