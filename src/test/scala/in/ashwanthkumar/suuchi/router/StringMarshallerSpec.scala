package in.ashwanthkumar.suuchi.router

import org.scalatest.FlatSpec
import org.scalatest.Matchers.{be, convertToAnyShouldWrapper}

class StringMarshallerSpec extends FlatSpec {
   "StringMarshaller" should "return the string as is when serialised" in {
     StringMarshaller.toAsciiString("suuchi") should be("suuchi")
   }

   it should "return the string as is when de-serialised" in {
     StringMarshaller.parseAsciiString("suuchi") should be("suuchi")
   }
 }
