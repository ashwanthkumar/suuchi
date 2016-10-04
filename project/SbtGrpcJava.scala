import java.io.{File, FileOutputStream}

import org.apache.commons.io.IOUtils
import sbt.Keys._
import sbt._
import sbtprotoc.ProtocPlugin.autoImport.PB

object SbtGrpcJava {

  def protobufSettings(moduleName: String) = Seq(
    PB.protoSources in Compile := Seq(file(s"$moduleName/src/main/proto")),
    PB.targets in Compile := Seq(
      PB.gens.java -> (sourceManaged in Compile).value // disable
    ),
    PB.protocOptions in Compile := Seq(
      "--plugin=protoc-gen-grpc-java=" + pluginPath(targetPath = (target in Compile).value.getAbsolutePath),
      "--grpc-java_out=" + (sourceManaged in Compile).value.getAbsolutePath
    )
  )

  /**
   * Downloads the protoc-grpc-plugin if not found locally and returns the path
   *
   * TODO - Extract this as an SBT plugin and release it
   */
  def pluginPath(targetPath: String, version: String = "1.0.1"): String = {
    val osClassifier = OSDetector.getClassifier
    val fileName = "protoc-gen-grpc-java-" + version + "-" + osClassifier + ".exe"
    val outputFile = new File(targetPath + "/protoc-plugins/" + fileName)
    outputFile.getParentFile.mkdirs()
    if (!outputFile.exists()) {
      downloadPlugin(version, fileName, outputFile)
    } else {
      println("GRPC Plugin found locally")
    }
    outputFile.setExecutable(true) // explicitly mark the file executable
    outputFile.getAbsolutePath
  }

  def downloadPlugin(version: String, fileName: String, outputFile: File): Unit = {
    val binaryUrl = sbt.url("http://repo1.maven.org/maven2/io/grpc/protoc-gen-grpc-java/" + version + "/" + fileName)
    val outputStream = new FileOutputStream(outputFile)
    println("Downloading " + binaryUrl.toString + " to " + outputFile.getAbsolutePath)
    IOUtils.copy(binaryUrl.openConnection().getInputStream, outputStream)
    IOUtils.closeQuietly(outputStream)
    println("Download of protoc-gen-grpc-java plugin is complete")
  }
}
