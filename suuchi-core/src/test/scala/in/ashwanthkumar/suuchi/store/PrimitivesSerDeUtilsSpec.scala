package in.ashwanthkumar.suuchi.store

import org.scalatest.FlatSpec
import org.scalatest.Matchers._

class PrimitivesSerDeUtilsSpec extends FlatSpec {
  "PrimitivesSerDe" should "perform serde on long as expected" in {
    PrimitivesSerDeUtils.bytesToLong(PrimitivesSerDeUtils.longToBytes(100l)) should be(100l)
    PrimitivesSerDeUtils.bytesToLong(PrimitivesSerDeUtils.longToBytes(-100l)) should be(-100l)
    PrimitivesSerDeUtils.bytesToLong(PrimitivesSerDeUtils.longToBytes(Long.MaxValue)) should be(Long.MaxValue)
    PrimitivesSerDeUtils.bytesToLong(PrimitivesSerDeUtils.longToBytes(Long.MinValue)) should be(Long.MinValue)
  }

  it should "perform serde on int as expected" in {
    PrimitivesSerDeUtils.bytesToInt(PrimitivesSerDeUtils.intToBytes(100)) should be(100)
    PrimitivesSerDeUtils.bytesToInt(PrimitivesSerDeUtils.intToBytes(-100)) should be(-100)
    PrimitivesSerDeUtils.bytesToInt(PrimitivesSerDeUtils.intToBytes(Int.MaxValue)) should be(Int.MaxValue)
    PrimitivesSerDeUtils.bytesToInt(PrimitivesSerDeUtils.intToBytes(Int.MinValue)) should be(Int.MinValue)
  }
}
