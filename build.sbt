import com.williamhill.bettingengine.sbt.{FileSystem, WHNexus}
import Dependencies._
import DockerSettings._

lazy val scalaVer = "2.13.8"

ThisBuild / scalaVersion  := scalaVer
ThisBuild / versionScheme := Some("early-semver")

// Scalafix configuration
ThisBuild / semanticdbEnabled                              := true
ThisBuild / semanticdbVersion                              := scalafixSemanticdb.revision
ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.6.0"
ThisBuild / scalafixScalaBinaryVersion                     := "2.13"

ThisBuild / javaOptions ++= Seq(
  // ZIO Interop Log4j2
  "-Dlog4j2.threadContextMap=com.github.mlangc.zio.interop.log4j2.FiberAwareThreadContextMap",
)

lazy val commonSettings = Seq(
  scalaVersion := scalaVer,
  organization := "com.williamhill.platform",
  resolvers ++= Seq(
    WHNexus.Releases,
    WHNexus.Snapshots,
    "Typesafe Releases" at "https://repo.typesafe.com/typesafe/releases/",
    "Confluent" at "https://packages.confluent.io/maven/",
    "jitpack.io" at "https://jitpack.io/",
  ),
  scalacOptions ++= List(
    "-unchecked",
    "-deprecation",
    "-encoding",
    "utf8",
    "-Xfatal-warnings",
    "-feature",
    "-Wunused:imports",
    "-P:kind-projector:underscore-placeholders",
    "-Xsource:3", // Enable simpler smart constructor - https://gist.github.com/tpolecat/a5cb0dc9adeacc93f846835ed21c92d2#gistcomment-3386246
  ),
  Compile / console / scalacOptions ~= (_.filterNot(Set("-Xfatal-warnings"))),
  libraryDependencies ++= List(
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
    williamHill.facetEvent,
    enumeratum.circe,
    http4s.dsl,
    kafkaSerdeScala.circe,
    testContainers.scalaKafka % IntegrationTest,
    logback.logstashLogbackEncoder,
    monocle.core,
    monocle.`macro`,
    "org.scalactic" %% "scalactic" % "3.2.12",
    "org.scalatest" %% "scalatest" % "3.2.12" % "it",
    compilerPlugin("org.typelevel" % "kind-projector" % "0.13.2" cross CrossVersion.full),
  ),
)

ThisBuild / credentials ++= WHNexus.optionalCredentialsFromEnv()

ThisBuild / credentials += Credentials(
  "Sonatype Nexus Repository Manager",
  "dev-jenkins-wh-01.whbettingengine.com",
  "wh",
  "8U0p8jAr6Me36vl",
)

val testSettings = Seq(
  Test / logBuffered        := false,
  Test / fork               := true,
  Test / testForkedParallel := true,
  Test / parallelExecution  := true,
  Test / javaOptions ++= Seq(
    "-Dconfig.resource=/test.conf",
    // ZIO Interop Log4j2
    "-Dlog4j2.threadContextMap=com.github.mlangc.zio.interop.log4j2.FiberAwareThreadContextMap",
    "-Dsbt.io.implicit.relative.glob.conversion=allow",
  ),
  testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
  coverageDataDir := crossTarget.value / "scoverage-report",
)

lazy val assemblySettings = List(
  assembly / assemblyMergeStrategy := {
    case x if Assembly.isConfigFile(x) => MergeStrategy.concat
    case PathList(ps @ _*) if Assembly.isReadme(ps.last) || Assembly.isLicenseFile(ps.last) =>
      MergeStrategy.rename
    case PathList("org", "joda", "convert", _*) => MergeStrategy.discard
    case PathList("META-INF", xs @ _*) =>
      xs.map(_.toLowerCase) match {
        case "manifest.mf" :: Nil | "index.list" :: Nil | "dependencies" :: Nil =>
          MergeStrategy.discard
        case ps @ (_ :: _) if ps.last.endsWith(".sf") || ps.last.endsWith(".dsa") =>
          MergeStrategy.discard
        case "plexus" :: _                                      => MergeStrategy.discard
        case "services" :: _                                    => MergeStrategy.filterDistinctLines
        case "spring.schemas" :: Nil | "spring.handlers" :: Nil => MergeStrategy.filterDistinctLines
        case _                                                  => MergeStrategy.first
      }
    case _ => MergeStrategy.first
  },
  assembly / test := {},
  publish / skip  := true,
)

lazy val permissionsEpMain = "com.williamhill.permission.Main"

lazy val `permissions-ep` = project
  .in(file("."))
  .settings(commonSettings: _*)
  .settings(testSettings: _*)
  .configs(IntegrationTest)
  .settings(Defaults.itSettings)
  .settings(assemblySettings: _*)
  .settings(Compile / mainClass := Some(permissionsEpMain))
  .settings(dockerSettings("permissions-ep", permissionsEpMain): _*)
  .enablePlugins(DockerPlugin)

Global / onChangedBuildSource := ReloadOnSourceChanges
