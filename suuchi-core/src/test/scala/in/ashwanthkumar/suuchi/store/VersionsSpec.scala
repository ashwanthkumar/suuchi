package in.ashwanthkumar.suuchi.store

import org.scalatest.FlatSpec
import org.scalatest.Matchers.{convertToAnyShouldWrapper, be}

import scala.util.Random

class VersionsSpec extends FlatSpec {
  "Versions" should "do List[Long] SerDe properly" in {
    val versionTs = Random.nextLong()
    val writtenTs = Random.nextLong()

    val serialised = Versions.toBytes(List(Version(versionTs, writtenTs)))
    Versions.fromBytes(serialised) should be(List(Version(versionTs, writtenTs)))
  }
}
