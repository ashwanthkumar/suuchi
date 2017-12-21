import sbt._

object Dependencies {
  val scalaTest = "org.scalatest" %% "scalatest"   % "3.0.0"   % Test
  val mockito   = "org.mockito"   % "mockito-core" % "1.10.19" % Test

  val algebird  = "com.twitter" %% "algebird-core"  % "0.13.0"
  val bijection = "com.twitter" %% "bijection-core" % "0.9.5"

  val joda      = "joda-time"    % "joda-time"  % "2.8.2"
  val hocon     = "com.typesafe" % "config"     % "1.2.1"
  val commonsIO = "commons-io"   % "commons-io" % "2.5"

  val grpcVersion  = "1.2.0"
  val grpcNetty    = "io.grpc" % "grpc-netty" % grpcVersion
  val grpcStub     = "io.grpc" % "grpc-stub" % grpcVersion
  val grpcCore     = "io.grpc" % "grpc-core" % grpcVersion
  val grpcProtobuf = "io.grpc" % "grpc-protobuf" % grpcVersion
  val grpcServices = "io.grpc" % "grpc-services" % grpcVersion
  val grpcTesting  = "io.grpc" % "grpc-testing" % grpcVersion % Test
  val grpc         = Seq(grpcNetty, grpcStub, grpcCore, grpcProtobuf, grpcServices, grpcTesting)

  val nettyVersion   = "4.1.8.Final"
  val nettyCodec     = "io.netty" % "netty-codec" % nettyVersion
  val nettyCommon    = "io.netty" % "netty-common" % nettyVersion
  val nettyTransport = "io.netty" % "netty-transport" % nettyVersion
  val nettyHandler   = "io.netty" % "netty-handler" % nettyVersion
  val netty          = Seq(nettyCodec, nettyCommon, nettyTransport, nettyHandler)

  val sbProtoRuntime = "com.trueaccord.scalapb" %% "scalapb-runtime" % com.trueaccord.scalapb.compiler.Version.scalapbVersion
  val sbGrpcRuntime  = "com.trueaccord.scalapb" %% "scalapb-runtime-grpc" % com.trueaccord.scalapb.compiler.Version.scalapbVersion
  val scalaPB        = Seq(sbProtoRuntime, sbGrpcRuntime)

  val slf4j = "org.slf4j" % "slf4j-api" % "1.7.12"

  val log4jVersion   = "2.6.2"
  val log4jCore      = "org.apache.logging.log4j" % "log4j-core" % log4jVersion
  val log4jApi       = "org.apache.logging.log4j" % "log4j-api" % log4jVersion
  val log4jOverSlf4j = "org.apache.logging.log4j" % "log4j-slf4j-impl" % log4jVersion
  val log4j          = Seq(log4jCore, log4jApi, log4jOverSlf4j)

  val rocksDBjni = "org.rocksdb" % "rocksdbjni" % "5.8.6"

  val atomixVersion     = "1.0.6"
  val catalystVersion   = "1.2.0"
  val atomixCore        = "io.atomix" % "atomix" % atomixVersion
  val atomixResource    = "io.atomix" % "atomix-resource" % atomixVersion
  val catalystTransport = "io.atomix.catalyst" % "catalyst-transport" % catalystVersion
  val catalystNetty     = "io.atomix.catalyst" % "catalyst-netty" % catalystVersion
  val atomix            = Seq(atomixCore, atomixResource, catalystTransport, catalystNetty)

  val scalecube = "io.scalecube" % "scalecube-cluster" % "0.9.0"
  val rxScala   = "io.reactivex" %% "rxscala"          % "0.26.2"

  val testDeps = Seq(scalaTest, mockito)

  lazy val coreDependencies = Seq(joda, slf4j, hocon, commonsIO, algebird) ++ scalaPB ++ grpc ++ netty ++ testDeps

  lazy val rocksDBDependencies = Seq(rocksDBjni) ++ testDeps

  lazy val examplesDependencies = Seq(slf4j) ++ log4j ++ testDeps

  lazy val atomixDependencies = atomix ++ testDeps

  lazy val scalecubeDependencies = Seq(scalecube, rxScala) ++ testDeps

}
