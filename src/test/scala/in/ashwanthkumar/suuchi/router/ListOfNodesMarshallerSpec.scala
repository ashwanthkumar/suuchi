package in.ashwanthkumar.suuchi.router

import in.ashwanthkumar.suuchi.membership.MemberAddress
import org.scalatest.FlatSpec
import org.scalatest.Matchers.{be, contain, convertToAnyShouldWrapper, have}

class ListOfNodesMarshallerSpec extends FlatSpec {
  "ListOfNodesMarshaller" should "convert list of members to ascii string" in {
    val members = List(
      MemberAddress("localhost", 5051),
      MemberAddress("localhost", 5052),
      MemberAddress("localhost", 5053)
    )

    MemberAddressMarshaller.toAsciiString(members) should be("localhost:5051|localhost:5052|localhost:5053")
  }

  it should "convert the ascii string to actual node objects" in {
    val members = MemberAddressMarshaller.parseAsciiString("localhost:5051|localhost:5052|localhost:5053")
    members should have size 3

    members should contain(MemberAddress("localhost", 5051))
    members should contain(MemberAddress("localhost", 5052))
    members should contain(MemberAddress("localhost", 5053))
  }
}
