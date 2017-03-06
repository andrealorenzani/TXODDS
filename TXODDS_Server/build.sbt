name := """play-scala"""

version := "1.0"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"

PlayKeys.devSettings := Seq("play.server.http.port" -> "9001")

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws
)
