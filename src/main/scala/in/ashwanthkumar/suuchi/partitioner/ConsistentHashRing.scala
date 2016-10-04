package in.ashwanthkumar.suuchi.partitioner

import java.util

import in.ashwanthkumar.suuchi.membership.MemberAddress

import scala.collection.mutable

// Ref - https://git.io/vPOP5
class ConsistentHashRing(hashFn: Hash, vnodeFactor: Int = 3) {
  val sortedMap = new util.TreeMap[Integer, VNode]()

  // when looking for n unique bins, give up after a streak of MAX_DUPES
  // duplicates
  val MAX_DUPES = 10


  def init(nodes: List[MemberAddress]) = {
    nodes.foreach(add)
    this
  }

  private def hash(vnode: VNode): Int = hashFn.hash(vnode.key.getBytes)

  def add(node: MemberAddress) = {
    (1 to vnodeFactor).map(i => VNode(node, i)).foreach { vnode =>
      sortedMap.put(hash(vnode), vnode)
    }
    this
  }

  def remove(node: MemberAddress) = {
    (1 to vnodeFactor).map(i => VNode(node, i)).foreach { vnode =>
      sortedMap.remove(hash(vnode))
    }
    this
  }

  def find(key: Array[Byte]): Option[MemberAddress] = {
    if (sortedMap.isEmpty) None
    else {
      val (_, candidate) = findCandidate(hashFn.hash(key))
      Some(candidate)
    }
  }

  /**
   * This returns the closest n bins in order for the object. There may be
   * duplicates.
   */
  def find(key: Array[Byte], n: Int) = {
    if (sortedMap.isEmpty) Nil
    else {
      var hash = hashFn.hash(key)
      (0 until n).map { index =>
        val (newHash, candidate) = findCandidate(hash)
        hash = newHash + 1

        candidate
      }.toList
    }
  }

  /**
   * This returns the closest n bins in order for the object. There is extra
   * code that forces the bin values to be unique.
   *
   * This will return a list that has all the bins (and is smaller than n) if n
   * > number of bins.
   */
  def findNUnique(key: Array[Byte], n: Int) = {
    if (sortedMap.isEmpty) Nil
    else {
      var duped = 0
      var hashIdx = hashFn.hash(key)
      var uniqueBins = mutable.MutableList[MemberAddress]()
      var index = 0
      while (index < n) {
        val (newHash, candidate) = findCandidate(hashIdx)
        hashIdx = newHash
        if (!uniqueBins.contains(candidate)) {
          duped = 0
          uniqueBins += candidate
        } else {
          duped += 1
          index -= 1 // try again
          if (duped > MAX_DUPES) {
            index += 1; // we've been duped too many times, just skip to next, returning
            // fewer than n
          }
        }

        // was a hit so we increment and loop to find the next bin in the
        // circle
        hashIdx += 1
        index += 1
      }
      uniqueBins.toList
    }
  }

  private[partitioner] def findCandidate(hash: Integer) = {
    if (sortedMap.containsKey(hash)) {
      hash -> sortedMap.get(hash).node
    } else {
      val tailMap = sortedMap.tailMap(hash)
      val newHash = if (tailMap.isEmpty) sortedMap.firstKey() else tailMap.firstKey()
      newHash -> sortedMap.get(newHash).node
    }
  }

  // USED ONLY FOR TESTS
  private[partitioner] def nodes = sortedMap.values()
}

object ConsistentHashRing {
  def apply(hashFn: Hash): ConsistentHashRing = new ConsistentHashRing(hashFn)

  def apply(): ConsistentHashRing = apply(SuuchiHash)

  def apply(nodes: List[MemberAddress]): ConsistentHashRing = apply(SuuchiHash).init(nodes)

  def apply(replication: Int): ConsistentHashRing = apply(SuuchiHash, replication)

  def apply(hashFn: Hash, replication: Int): ConsistentHashRing = new ConsistentHashRing(hashFn, replication)
}
