import sbt._

object Dependencies {
  val scalaTest = "org.scalatest" %% "scalatest"   % "2.2.5"   % Test
  val mockito   = "org.mockito"   % "mockito-core" % "1.10.19" % Test

  val algebird = "com.twitter" %% "algebird-core" % "0.13.0"
  val bijection = "com.twitter" %% "bijection-core" % "0.9.5"

  val joda  = "joda-time"         % "joda-time" % "2.8.2"
  val hocon     = "com.typesafe" % "config"     % "1.2.1"
  val commonsIO = "commons-io"   % "commons-io" % "2.5"

  val suuchiVersion = "0.3.5"
  val suuchi        = "in.ashwanthkumar" % "suuchi-core" % suuchiVersion
  val suuchiAtomix   = "in.ashwanthkumar" % "suuchi-cluster-atomix" % suuchiVersion
  val suuchiRocks   = "in.ashwanthkumar" % "suuchi-rocksdb" % suuchiVersion exclude ("org.rocksdb", "rocksdbjni")
  val rocksDB       = "org.rocksdb" % "rocksdbjni" % "5.8.0"

  val grpcVersion = "1.2.0"
  val grpcNetty   = "io.grpc" % "grpc-netty" % grpcVersion
  val grpcStub    = "io.grpc" % "grpc-stub" % grpcVersion
  val grpcCore    = "io.grpc" % "grpc-core" % grpcVersion
  val grpcProtobuf    = "io.grpc" % "grpc-protobuf" % grpcVersion
  val grpcServices    = "io.grpc" % "grpc-services" % grpcVersion
  val grpc = Seq(grpcNetty, grpcStub, grpcCore, grpcProtobuf, grpcServices)

  val nettyVersion = "4.1.8.Final"
  val nettyCodec = "io.netty" % "netty-codec" % nettyVersion
  val nettyCommon = "io.netty" % "netty-common" % nettyVersion
  val nettyTransport = "io.netty" % "netty-transport" % nettyVersion
  val nettyHandler = "io.netty" % "netty-handler" % nettyVersion
  val netty = Seq(nettyCodec, nettyCommon, nettyTransport, nettyHandler)

  val sbProtoRuntime = "com.trueaccord.scalapb" %% "scalapb-runtime"      % com.trueaccord.scalapb.compiler.Version.scalapbVersion
  val sbGrpcRuntime  = "com.trueaccord.scalapb" %% "scalapb-runtime-grpc" % com.trueaccord.scalapb.compiler.Version.scalapbVersion

  val slf4j = "org.slf4j" % "slf4j-api" % "1.7.12"

  val testDeps = Seq(scalaTest, mockito)

  val coreDependencies = Seq(joda, slf4j, hocon, commonsIO, algebird) ++ grpc ++ netty ++ testDeps

}
