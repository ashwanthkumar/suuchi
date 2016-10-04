addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.1")

libraryDependencies += "commons-io" % "commons-io" % "2.4"

// sbt couldn't seem to pick this artifact unless explicitly added from the url - :-/
libraryDependencies += "kr.motd.maven" % "os-maven-plugin" % "1.4.0.Final" from "http://central.maven.org/maven2/kr/motd/maven/os-maven-plugin/1.4.0.Final/os-maven-plugin-1.4.0.Final.jar"
