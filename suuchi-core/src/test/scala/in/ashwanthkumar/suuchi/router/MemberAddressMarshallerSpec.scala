package in.ashwanthkumar.suuchi.router

import in.ashwanthkumar.suuchi.cluster.MemberAddress
import org.scalatest.FlatSpec
import org.scalatest.Matchers.{be, contain, convertToAnyShouldWrapper, have}

class MemberAddressMarshallerSpec extends FlatSpec {
  "MemberAddressMarshaller" should "convert a member to ascii string" in {
    MemberAddressMarshaller.toAsciiString(MemberAddress("localhost", 5051)) should be(
      "localhost:5051")
  }

  it should "convert the ascii string to actual member address" in {
    val member = MemberAddressMarshaller.parseAsciiString("localhost:5051")
    member should be(MemberAddress("localhost", 5051))
  }
}
