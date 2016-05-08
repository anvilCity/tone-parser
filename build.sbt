name := "anvil-tone-parser"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies += "io.scalac" %% "reactive-rabbit" % "1.1.0"
libraryDependencies += "com.typesafe" % "config" % "1.3.0"
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.4.4"
libraryDependencies += "com.typesafe.play" % "play-json_2.11" % "2.5.0"
libraryDependencies += "com.typesafe.akka" % "akka-http-experimental_2.11" % "2.4.4"
libraryDependencies += "com.sksamuel.elastic4s" % "elastic4s-core_2.11" % "2.3.0"
libraryDependencies += "com.sksamuel.elastic4s" %% "elastic4s-streams" % "1.7.4"


val aProject = project.in(file(".")).enablePlugins(UniversalPlugin)
