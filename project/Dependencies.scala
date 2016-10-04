import sbt._

object Dependencies {
  val scalatest = "org.scalatest" %% "scalatest" % "2.1.3" % "test"
  val mockito = "org.mockito" % "mockito-core" % "1.10.19" % "test"

  val test = Seq(scalatest, mockito)

  val commonsIO = "commons-io" % "commons-io" % "2.4"

  val AtomixVersion = "1.0.0-rc9"
  val atomixCore = "io.atomix" % "atomix" % AtomixVersion
  val atomixResource = "io.atomix" % "atomix-resource" % AtomixVersion
  val atomix = Seq(atomixCore, atomixResource)

  val CatalystVersion = "1.1.1"
  val catalystTransport = "io.atomix.catalyst" % "catalyst-transport" % CatalystVersion
  val catalystNetty = "io.atomix.catalyst" % "catalyst-netty" % CatalystVersion
  val catalyst = Seq(catalystTransport, catalystNetty)

  val GrpcVersion = "1.0.1"
  val grpcNetty = "io.grpc" % "grpc-netty" % GrpcVersion
  val grpcProtobuf = "io.grpc" % "grpc-protobuf" % GrpcVersion
  val grpcStub = "io.grpc" % "grpc-stub" % GrpcVersion
  val grpcServices = "io.grpc" % "grpc-services" % GrpcVersion
  val grpc = Seq(grpcNetty, grpcProtobuf, grpcStub, grpcServices)

  val scalapbRuntime = "com.trueaccord.scalapb" %% "scalapb-runtime-grpc" % "0.5.42"

  val NettyVersion = "4.1.3.Final"
  val nettyCodec = "io.netty" % "netty-codec" % NettyVersion
  val nettyCommon = "io.netty" % "netty-common" % NettyVersion
  val nettyTransport = "io.netty" % "netty-transport" % NettyVersion
  val nettyHandler = "io.netty" % "netty-handler" % NettyVersion
  val netty = Seq(nettyCodec, nettyCommon, nettyTransport, nettyTransport)

  val slf4j = "org.slf4j" % "slf4j-api" % "1.7.12"
  val Log4JVersion = "2.6.2"
  val log4jCore = "org.apache.logging.log4j" % "log4j-core" % Log4JVersion
  val log4jSlf4jImpl = "org.apache.logging.log4j" % "log4j-slf4j-impl" % Log4JVersion
  val logging = Seq(slf4j, log4jCore, log4jSlf4jImpl)

  val rocksDB = "org.rocksdb" % "rocksdbjni" % "4.9.0"

  val core = test ++ Seq(commonsIO, rocksDB, scalapbRuntime) ++ grpc ++ netty ++ logging ++ atomix ++ catalyst
}
