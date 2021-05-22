ThisBuild / scalaVersion     := "2.13.6"
ThisBuild / version          := "0.1.0-SNAPSHOT"

val akkaVersion = "2.6.14"

lazy val root = (project in file("."))
  .settings(
    name := "agenty",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-persistence-typed" % akkaVersion,
      "org.slf4j" % "slf4j-simple" % "1.7.30",
      "org.iq80.leveldb" % "leveldb" % "0.9",
      "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8",
    )
  )