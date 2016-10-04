package in.ashwanthkumar.suuchi.router

import in.ashwanthkumar.suuchi.membership.MemberAddress
import org.scalatest.FlatSpec
import org.scalatest.Matchers.{be, contain, convertToAnyShouldWrapper, have}

class StringMarshallerSpec extends FlatSpec {
   "StringMarshaller" should "return the string as is when serialised" in {
     StringMarshaller.toAsciiString("suuchi") should be("suuchi")
   }

   it should "return the string as is when de-serialised" in {
     StringMarshaller.parseAsciiString("suuchi") should be("suuchi")
   }
 }
