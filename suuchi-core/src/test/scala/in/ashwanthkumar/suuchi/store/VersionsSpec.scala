package in.ashwanthkumar.suuchi.store

import org.scalatest.FlatSpec
import org.scalatest.Matchers.{convertToAnyShouldWrapper, be}

import scala.util.Random

class VersionsSpec extends FlatSpec {
  "Versions" should "do List[Long] SerDe properly" in {
    val a = Random.nextLong()
    val b = Random.nextLong()

    val serialised = Versions.toBytes(List(a, b))
    Versions.fromBytes(serialised) should be(List(a, b))
  }
}
