import scala.sys.process.Process
import scala.util.Try
import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.Date

import Dependencies._
import sbt.Keys._
import sbt.Package.ManifestAttributes
import sbt.SbtExclusionRule


ivyScala := ivyScala.value map {
  _.copy(overrideScalaVersion = true)
}

val scalaV = "2.11.11"
val gitRevision = Try(Process("git rev-parse HEAD").!!.stripLineEnd).getOrElse("?").trim.take(6)
val buildVersion = sys.env.getOrElse("GO_PIPELINE_LABEL", "1.0.0-SNAPSHOT-" + gitRevision)

lazy val core = (project in file("suuchi-core"))
  .settings(
    name := "suuchi-core",
    libraryDependencies ++= coreDependencies
  )
  .settings(protoConfigurations: _*)
  .settings(projectSettings: _*)
  .settings(publishSettings: _*)
  .settings(buildInfoSettings: _*)
  .enablePlugins(BuildInfoPlugin)

lazy val rocksStore = (project in file("suuchi-rocksdb"))
  .settings(
    name := "suuchi-rocksdb",
    libraryDependencies ++= rocksDBDependencies
  )
  .settings(projectSettings: _*)
  .settings(publishSettings: _*)
  .dependsOn(core)

lazy val examples = (project in file("suuchi-examples"))
  .settings(
    name := "suuchi-examples",
    libraryDependencies ++= examplesDependencies
  )
  .settings(protoConfigurations: _*)
  .settings(projectSettings: _*)
  .settings(publishSettings: _*)
  .dependsOn(core, rocksStore)

lazy val clusterAtomix = (project in file("suuchi-cluster-atomix"))
  .settings(
    name := "suuchi-cluster-atomix",
    libraryDependencies ++= atomixDependencies
  )
  .settings(projectSettings: _*)
  .settings(publishSettings: _*)
  .dependsOn(core)

lazy val clusterScalecube = (project in file("suuchi-cluster-scalecube"))
  .settings(
    name := "suuchi-cluster-scalecube",
    libraryDependencies ++= scalecubeDependencies
  )
  .settings(projectSettings: _*)
  .settings(publishSettings: _*)
  .dependsOn(core)

lazy val buildInfoSettings = Seq(
  buildInfoPackage := "in.ashwanthkumar.suuchi.version",
  buildInfoObject := "SuuchiBuildInfo",
  buildInfoUsePackageAsPath := true,
  buildInfoKeys := Seq[BuildInfoKey](
    BuildInfoKey.action("buildDate")(new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date())),
    BuildInfoKey.action("buildVersion")(buildVersion),
    BuildInfoKey.action("buildSha")(gitRevision)
  )
)

lazy val projectSettings = net.virtualvoid.sbt.graph.Plugin.graphSettings ++ Seq(
  version := buildVersion,
  organization := "in.ashwanthkumar",
  scalaVersion := scalaV,
  resolvers += Resolver.mavenLocal,
  excludeDependencies ++= Seq(
    SbtExclusionRule("cglib", "cglib-nodep"),
    SbtExclusionRule("commons-beanutils", "commons-beanutils"),
    SbtExclusionRule("commons-beanutils", "commons-beanutils-core")
  ),
  parallelExecution in ThisBuild := false,
  scalacOptions ++= Seq("-unchecked",
    "-feature"
    // , "-Ylog-classpath" // useful while debugging dependency classpath issues
  )
)

lazy val publishSettings = Seq(
  publishArtifact := true,
  packageOptions := Seq(
    ManifestAttributes(
      ("Built-By", InetAddress.getLocalHost.getHostName)
    )),
  crossScalaVersions := Seq(scalaV),
  publishMavenStyle := true,
  // disable publishing test jars
  publishArtifact in Test := false,
  // disable publishing the main docs jar
  publishArtifact in(Compile, packageDoc) := false,
  // disable publishing the main sources jar
  publishArtifact in(Compile, packageSrc) := true,
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
  pomExtra :=
    <url>https://github.com/ashwanthkumar/suuchi</url>
      <licenses>
        <license>
          <name>Apache2</name>
          <url>http://www.apache.org/licenses/LICENSE-2.0</url>
        </license>
      </licenses>
      <scm>
        <url>https://github.com/ashwanthkumar/suuchi</url>
        <connection>scm:git:https://github.com/ashwanthkumar/suuchi.git</connection>
        <tag>HEAD</tag>
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
)

lazy val protoConfigurations: Seq[Def.Setting[_]] = Seq(
  // Reset the managedSourceDirectories list with just protobuf so we don't have the parent main directory
  // in the classpath which causes issues in IDEA everytime we refresh the project
  managedSourceDirectories in Compile ++= Seq((target in Compile).value / "protobuf-generated"),
  PB.targets in Compile := Seq(
    scalapb.gen(flatPackage = true) -> (target in Compile).value / "protobuf-generated"
  )
) ++ inConfig(Test)(sbtprotoc.ProtocPlugin.protobufConfigSettings)