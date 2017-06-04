package in.ashwanthkumar.suuchi.utils

import org.scalatest.matchers.{BeMatcher, MatchResult}

class DistinctMatcher[T] extends BeMatcher[List[T]] {
  override def apply(left: List[T]): MatchResult = {
    MatchResult(left.distinct.size == left.size, "is not distinct", "is distinct")
  }
}
