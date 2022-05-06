import com.williamhill.bettingengine.sbt.WHNexus

import Dependencies._
ThisBuild / scalaVersion := "2.13.8"
ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.williamhill.platform"
ThisBuild / name         := "permission-service"

ThisBuild / resolvers ++= Seq(
  WHNexus.Releases,
  WHNexus.Snapshots,
  WHNexus.Confluent,
  "Typesafe Releases" at "https://repo.typesafe.com/typesafe/releases/",
  "Confluent" at "https://packages.confluent.io/maven/",
  "jitpack.io" at "https://jitpack.io/",
)
ThisBuild / libraryDependencies ++= List(
  zio.core,
  zio.slf4zio,
  zio.test,
  zio.`test-sbt`,
  zio.`test-scalacheck`,
  zio.kafka,
  zio.magic,
  zio.stream,
  scalaTest,
  circe.core,
  circe.generic,
  circe.jawn,
  confluent.jsonSchemaSerializer,
  confluent.schemaRegistryClient,
  andyglow.core,
  andyglow.circeJson,
  andyglow.jsonSchemaCats,
  kamon.bundle,
  kamon.newRelic,
  kamon.newRelicAppender,
  williamHill.kafkaLibraryCore,
  williamHill.kafkaExtensions,
  williamHill.healthcheck.http4s,
  williamHill.healthcheck.kafka,
  williamHill.tracing.tracing,
  williamHill.tracing.tracingKafka,
  enumeratum.circe,
  http4s.dsl,
  compilerPlugin("org.typelevel" % "kind-projector" % "0.13.2" cross CrossVersion.full),
)

ThisBuild / credentials ++= WHNexus.optionalCredentialsFromEnv()
ThisBuild / credentials += Credentials(
  "Sonatype Nexus Repository Manager",
  "dev-jenkins-wh-01.whbettingengine.com",
  "wh",
  "8U0p8jAr6Me36vl",
)
ThisBuild / scalacOptions ++= List(
  "-unchecked",
  "-deprecation",
  "-encoding",
  "utf8",
  "-Xfatal-warnings",
  "-feature",
  "-Wunused:imports",
  "-P:kind-projector:underscore-placeholders",
  "-Xsource:3", // Enable simpler smart constructor - https://gist.github.com/tpolecat/a5cb0dc9adeacc93f846835ed21c92d2#gistcomment-3386246
)

lazy val root = project in file(".")
Global / onChangedBuildSource := ReloadOnSourceChanges
