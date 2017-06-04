package in.ashwanthkumar.suuchi.partitioner

import in.ashwanthkumar.suuchi.cluster.MemberAddress
import in.ashwanthkumar.suuchi.utils.DistinctMatcher
import org.scalatest.FlatSpec
import org.scalatest.Matchers._

import scala.collection.mutable

class NodePriorityTest extends FlatSpec {

  val distinct = new DistinctMatcher[MemberAddress]

  "NodePriority" should "init the queue with nodes" in {
    val priorityQueue = new NodePriority()
    val nodes = List(MemberAddress("host1", 1), MemberAddress("host2", 2), MemberAddress("host3", 3))

    priorityQueue.init(nodes)

    priorityQueue.values() should have size 3

    priorityQueue.add(MemberAddress("host4", 4))

    priorityQueue.values() should have size 4

    priorityQueue.add(WeightedNode(MemberAddress("host5", 5), 10))

    priorityQueue.values() should have size 5
  }

  it should "decrease a node's weight when it is de-prioritized" in {
    val priorityQueue = new NodePriority()
    val nodes = List(MemberAddress("host1", 1), MemberAddress("host2", 2), MemberAddress("host3", 3))

    priorityQueue.init(nodes)
    val weightedNode = priorityQueue.dequeue()

    priorityQueue.dePrioritize(weightedNode)

    priorityQueue.values().filter(_.node.equals(weightedNode.node)).head.weight should be < weightedNode.weight
  }

  it should "give nodes in prioritized order when de-queued" in {
    val priorityQueue = new NodePriority()
    val nodes = List(MemberAddress("host1", 1), MemberAddress("host2", 2), MemberAddress("host3", 3))
    val leastWeighed = WeightedNode(MemberAddress("host4", 4), 4)

    priorityQueue.init(nodes)
    val weightedNode = priorityQueue.dequeue()
    priorityQueue.dePrioritize(weightedNode)
    priorityQueue.add(leastWeighed)

    priorityQueue.dequeue().node should not be weightedNode.node
    priorityQueue.dequeue().node should not be weightedNode.node
    priorityQueue.dequeue().node should be (weightedNode.node)
    priorityQueue.dequeue().node should be (leastWeighed.node)
  }

  it should "return back the requested number of follower nodes for a given node based on priority to support even distribution of nodes" in {
    val priorityQueue = new NodePriority()
    val initialWeight = 10
    (1 to 10).map(i => WeightedNode(MemberAddress(s"host$i", i), initialWeight)).foreach(priorityQueue.add)
    val followersAssignedSoFar = mutable.Set[MemberAddress]()

    val node1Followers = priorityQueue.followers(2, MemberAddress("host1", 1)).toSet
    node1Followers should have size 2
    followersAssignedSoFar.intersect(node1Followers) shouldBe empty
    followersAssignedSoFar ++= node1Followers

    val node2Followers = priorityQueue.followers(2, MemberAddress("host2", 2)).toSet
    node2Followers should have size 2
    followersAssignedSoFar.intersect(node2Followers) shouldBe empty
    followersAssignedSoFar ++= node2Followers

    val node3Followers = priorityQueue.followers(2, MemberAddress("host3", 3)).toSet
    node3Followers should have size 2
    followersAssignedSoFar.intersect(node3Followers) shouldBe empty
    followersAssignedSoFar ++= node3Followers

    val node4Followers = priorityQueue.followers(2, MemberAddress("host4", 4)).toSet
    node4Followers should have size 2
    followersAssignedSoFar.intersect(node4Followers) shouldBe empty
    followersAssignedSoFar ++= node4Followers

    val node5Followers = priorityQueue.followers(2, MemberAddress("host5", 5)).toSet
    node5Followers should have size 2
    followersAssignedSoFar.intersect(node5Followers) shouldBe empty
    followersAssignedSoFar ++= node5Followers

    followersAssignedSoFar should have size 10
  }

  it should "not return a node as a follower of itself" in {
    val priorityQueue = new NodePriority()
    val initialWeight = 10
    (1 to 10).map(i => WeightedNode(MemberAddress(s"host$i", i), initialWeight)).foreach(priorityQueue.add)

    priorityQueue.followers(9, MemberAddress("host1", 1)) shouldNot contain (MemberAddress("host1", 1))
    priorityQueue.followers(9, MemberAddress("host5", 5)) shouldNot contain (MemberAddress("host5", 5))
    priorityQueue.followers(9, MemberAddress("host10", 10)) shouldNot contain (MemberAddress("host10", 10))
  }

  it should "followers should be distinct, ie, there shouldn't be a duplicate entry in the result even though the next de-queued item has the highest priority" in {
    val priorityQueue = new NodePriority()
    val initialWeight = 10
    (1 to 10).map(i => WeightedNode(MemberAddress(s"host$i", i), initialWeight)).foreach(priorityQueue.add)
    priorityQueue.followers(3, MemberAddress("host1", 1)) shouldBe distinct
    priorityQueue.followers(3, MemberAddress("host2", 2)) shouldBe distinct
    priorityQueue.followers(3, MemberAddress("host3", 3)) shouldBe distinct
    priorityQueue.followers(3, MemberAddress("host4", 4)) shouldBe distinct
    priorityQueue.followers(3, MemberAddress("host5", 5)) shouldBe distinct
  }

}
