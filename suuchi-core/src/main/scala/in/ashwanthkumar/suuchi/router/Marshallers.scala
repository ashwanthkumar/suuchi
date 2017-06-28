package in.ashwanthkumar.suuchi.router

import in.ashwanthkumar.suuchi.cluster.MemberAddress
import io.grpc.Metadata.AsciiMarshaller

/**
 * Send a string value using AsciiMarshaller
 */
case object StringMarshaller extends AsciiMarshaller[String] {
  override def toAsciiString(value: String): String         = value
  override def parseAsciiString(serialized: String): String = serialized
}

/**
 * Send a boolean value using AsciiMarshaller
 */
case object BooleanMarshaller extends AsciiMarshaller[Boolean] {
  override def toAsciiString(value: Boolean): String         = value.toString
  override def parseAsciiString(serialized: String): Boolean = serialized.toBoolean
}

/**
 * Converts a collection of [[MemberAddress]] to it's external form separated by `|`
 */
case object ListOfMemberAddressMarshaller extends AsciiMarshaller[List[MemberAddress]] {
  override def parseAsciiString(serialized: String): List[MemberAddress] =
    serialized.split('|').map(MemberAddress.apply).toList
  override def toAsciiString(value: List[MemberAddress]): String =
    value.map(_.toExternalForm).mkString("|")
}

/**
 * Converts a [[MemberAddress]] to it's external form
 */
case object MemberAddressMarshaller extends AsciiMarshaller[MemberAddress] {
  override def parseAsciiString(serialized: String): MemberAddress =
    MemberAddress.apply(serialized)
  override def toAsciiString(value: MemberAddress): String =
    value.toExternalForm
}
