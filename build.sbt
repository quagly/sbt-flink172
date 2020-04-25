ThisBuild / resolvers ++= Seq(
    "Apache Development Snapshot Repository" at "https://repository.apache.org/content/repositories/snapshots/",
    Resolver.mavenLocal
)

name := "sbt-flink172"

version := "0.1"

organization := "com.mitzit"

ThisBuild / scalaVersion := "2.12.11"

lazy val flinkVersion = "1.7.2"
lazy val scalatestVersion = "3.1.1"
lazy val scalaLoggingVersion = "3.9.2"
lazy val logbackClassicVersion= "1.2.3"

val flinkDependencies = Seq(
  "org.apache.flink" %% "flink-scala" % flinkVersion % "provided",
  "org.apache.flink" %% "flink-streaming-scala" % flinkVersion % "provided"
)

val loggingDependencies = Seq(
  "com.typesafe.scala-logging" %% "scala-logging" % scalaLoggingVersion,
  "ch.qos.logback" % "logback-classic" % logbackClassicVersion
)

val testDependencies = Seq(
  "org.scalatest" %% "scalatest" % scalatestVersion % "test",
  "org.apache.flink" %% "flink-test-utils" % flinkVersion % "test"
)

lazy val root = (project in file(".")).
  settings(
    libraryDependencies ++= flinkDependencies,
    libraryDependencies ++= loggingDependencies,
    libraryDependencies ++= testDependencies
  )

assembly / mainClass := Some("com.mitzit.Job")

// make run command include the provided dependencies
Compile / run  := Defaults.runTask(Compile / fullClasspath,
                                   Compile / run / mainClass,
                                   Compile / run / runner
                                  ).evaluated

// stays inside the sbt console when we press "ctrl-c" while a Flink programme executes with "run" or "runMain"
Compile / run / fork := true
Global / cancelable := true

// exclude Scala library from assembly
assembly / assemblyOption  := (assembly / assemblyOption).value.copy(includeScala = false)
