import com.williamhill.bettingengine.sbt.WHNexus
import DockerSettings._

lazy val scala213 = "2.13.8"

ThisBuild / scalaVersion  := scala213
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

//TODO migrate to ZIO.2.0
val ZioVersion = "1.0.13"

val CirceVersion           = "0.14.1"
val KafkaSerdeCirceVersion = "0.6.5"
val ZioTracingVersion      = "0.1.1"
val JsonSchemaVersion      = "0.7.8"
val KamonVersion           = "2.2.2"
val MonocleVersion         = "3.1.0"

lazy val commonSettings = Seq(
  scalaVersion := scala213,
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
    "dev.zio" %% "zio"                 % ZioVersion,
    "dev.zio" %% "zio-streams"         % ZioVersion,
    "dev.zio" %% "zio-test-sbt"        % ZioVersion % Test,
    "dev.zio" %% "zio-test-scalacheck" % ZioVersion % Test,
    "dev.zio" %% "zio-test"            % ZioVersion % Test,
    // TODO remove scalatest
    "org.scalatest"                      %% "scalatest"                    % "3.2.11" % Test,
    "dev.zio"                            %% "zio-kafka"                    % "0.17.5",
    "io.github.kitlangton"               %% "zio-magic"                    % "0.3.12",
    "io.circe"                           %% "circe-core"                   % CirceVersion,
    "io.circe"                           %% "circe-generic"                % CirceVersion,
    "io.circe"                           %% "circe-jawn"                   % CirceVersion,
    "io.github.azhur"                    %% "kafka-serde-circe"            % "0.6.5",
    "io.confluent"                        % "kafka-json-schema-serializer" % "7.0.1",
    "io.confluent"                        % "kafka-schema-registry-client" % "5.3.0",
    "com.github.andyglow"                %% "scala-jsonschema-core"        % JsonSchemaVersion,
    "com.github.andyglow"                %% "scala-jsonschema-circe-json"  % JsonSchemaVersion,
    "com.github.andyglow"                %% "scala-jsonschema-cats"        % JsonSchemaVersion,
    "io.kamon"                           %% "kamon-bundle"                 % KamonVersion,
    "io.kamon"                           %% "kamon-newrelic"               % KamonVersion,
    "com.williamhill.platform.libraries" %% "newrelic-log-appender"        % "0.1.4",
    "com.williamhill.bettingengine"      %% "be-kafka-library-core"        % "1.2.8" excludeAll (ExclusionRule(
      "com.typesafe.akka",
      "akka-stream-kafka",
    )),
    "com.williamhill.platform"         %% "zio-kafka-extensions"     % "0.4.0-SNAPSHOT",
    "com.williamhill.platform"         %% "http4s-healthcheck"       % "0.1.10",
    "com.williamhill.platform"         %% "kafka-healthcheck"        % "0.1.10",
    "com.williamhill.platform.tracing" %% "zio-tracing"              % ZioTracingVersion,
    "com.williamhill.platform.tracing" %% "zio-tracing-kafka"        % "0.1.1",
    "com.williamhill.platform"         %% "facet-event-model"        % "0.9.4",
    "com.beachape"                     %% "enumeratum-circe"         % "1.7.0",
    "org.http4s"                       %% "http4s-dsl"               % "0.23.7", // TODO replace by ZIO http
    "net.logstash.logback"              % "logstash-logback-encoder" % "7.2",
    "dev.optics"                       %% s"monocle-core"            % MonocleVersion,
    "dev.optics"                       %% s"monocle-macro"           % MonocleVersion,
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
  // TODO validate this needed ??? replace logback by log4j ???
  Test / javaOptions ++= Seq(
    // ZIO Interop Log4j2
    "-Dlog4j2.threadContextMap=com.github.mlangc.zio.interop.log4j2.FiberAwareThreadContextMap",
    "-Dsbt.io.implicit.relative.glob.conversion=allow",
  ),
  testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
  coverageDataDir := crossTarget.value / "scoverage-report",
)

//TODO remove use PackPlugin
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

lazy val `permissions-ep` = project
  .in(file("."))
  .settings(commonSettings: _*)
  .settings(testSettings: _*)
  .settings(assemblySettings: _*)
  .settings(Compile / mainClass := Some("com.williamhill.permission.PermissionEp"))
  .settings(dockerSettings("permissions-ep", "com.williamhill.permission.PermissionEp"): _*)
  .enablePlugins(DockerPlugin)

Global / onChangedBuildSource := ReloadOnSourceChanges
