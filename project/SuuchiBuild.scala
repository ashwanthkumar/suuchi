import sbt.Keys._
import sbt._

object SuuchiBuild extends Build {

  val AppVersion = "0.1-SNAPSHOT"
  val ScalaVersion = "2.10.6"

  lazy val suuchi = Project("suuchi", file("."), settings = defaultSettings)
    .settings(organization := "in.ashwanthkumar",
      version := AppVersion,
      libraryDependencies ++= Dependencies.core
    ).settings(
    SbtGrpcJava.protobufSettings("."): _*
  )


  lazy val defaultSettings = super.settings ++ Seq(
    fork in run := false,
    parallelExecution in This := false,
    publishMavenStyle := true,
    publishArtifact in Test := false,
    publishArtifact in(Compile, packageDoc) := true,
    publishArtifact in(Compile, packageSrc) := true,
    resolvers += "Local Maven Repository" at "file://" + Path.userHome.absolutePath + "/.m2/repository",
    resolvers += "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/",
    publishTo <<= version { (v: String) =>
      val nexus = "https://oss.sonatype.org/"
      if (v.trim.endsWith("SNAPSHOT"))
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases" at nexus + "service/local/staging/deploy/maven2")
    },
    pomExtra := _pomExtra
  )

  val _pomExtra =
    <description>Toolkit to build distributed Data Systems</description>
      <url>https://github.com/ashwanthkumar/suuchi</url>
      <licenses>
        <license>
          <name>Apache2</name>
          <url>http://www.apache.org/licenses/LICENSE-2.0</url>
        </license>
      </licenses>
      <scm>
        <url>https://github.com/ashwanthkumar/suuchi</url>
        <connection>scm:git:git@github.com:ashwanthkumar/suuchi.git</connection>
      </scm>
      <developers>
        <developer>
          <email>ashwanthkumar@googlemail.com</email>
          <name>Ashwanth Kumar</name>
          <url>https://ashwanthkumar.in/</url>
          <id>ashwanthkumar</id>
        </developer>
        <developer>
          <email>sri.rams85@gmail.com</email>
          <name>Sriram Ramachandrasekaran</name>
          <id>brewkode</id>
        </developer>
      </developers>

}
