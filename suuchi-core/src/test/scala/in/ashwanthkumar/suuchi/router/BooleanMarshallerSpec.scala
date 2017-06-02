package in.ashwanthkumar.suuchi.router

import org.scalatest.FlatSpec
import org.scalatest.Matchers.{be, convertToAnyShouldWrapper}

class BooleanMarshallerSpec extends FlatSpec {
  "BooleanMarshaller" should "return the bool as string when serialised" in {
    BooleanMarshaller.toAsciiString(true) should be("true")
  }

  it should "return the bool when de-serialised" in {
    BooleanMarshaller.parseAsciiString("true") should be(true)
  }
}
