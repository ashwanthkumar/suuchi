package in.ashwanthkumar.suuchi.partitioner

import in.ashwanthkumar.suuchi.cluster.MemberAddress

import scala.collection.mutable

case class WeightedNode(node: MemberAddress, weight: Int) extends Ordered[WeightedNode] {
  override def compare(that: WeightedNode): Int = this.weight.compareTo(that.weight)
}

class NodePriority {
  private val queue = mutable.PriorityQueue.empty[WeightedNode]
  private val initialWeight = Integer.MAX_VALUE

  def init(nodes: List[MemberAddress]) = {
    nodes.foreach(node => queue.enqueue(WeightedNode(node, initialWeight)))
  }

  def add(node: MemberAddress) = {
    queue.enqueue(WeightedNode(node, initialWeight))
  }

  def add(weightedNode: WeightedNode) = {
    queue.enqueue(weightedNode)
  }

  def dePrioritize(weightedNode: WeightedNode) = {
    queue.enqueue(WeightedNode(weightedNode.node, weightedNode.weight - 1))
  }

  def allFollowers(number: Int, currentNode: MemberAddress, followers: List[MemberAddress], adjustQueue: List[WeightedNode]): List[MemberAddress] = {
    if (number == 0) {
      adjustQueue.foreach(add)
      followers
    }
    else {
      val weightedNode = queue.dequeue()
      if (weightedNode.node.equals(currentNode) || followers.contains(weightedNode.node)) {
        allFollowers(number, currentNode, followers, weightedNode :: adjustQueue)
      }
      else {
        dePrioritize(weightedNode)
        allFollowers(number - 1, currentNode, weightedNode.node :: followers, adjustQueue)
      }
    }
  }

  def followers(numberOfFollowers: Int, node: MemberAddress) = allFollowers(numberOfFollowers, node, List(), List())

  def values() = queue.toList

  def dequeue() = queue.dequeue()

}
