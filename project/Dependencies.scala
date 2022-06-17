import sbt._

object Dependencies {

  val scalaTest = "org.scalatest" %% "scalatest" % "3.2.11" % Test
  object zio {
    private val version = "1.0.13"

    val core  = "dev.zio" %% "zio"       % version
    val kafka = "dev.zio" %% "zio-kafka" % "0.17.4"

    val magic             = "io.github.kitlangton" %% "zio-magic"           % "0.3.12"
    val stream            = "dev.zio"              %% "zio-streams"         % version
    val test              = "dev.zio"              %% "zio-test"            % version % Test
    val `test-sbt`        = "dev.zio"              %% "zio-test-sbt"        % version % Test
    val `test-scalacheck` = "dev.zio"              %% "zio-test-scalacheck" % version % Test

    val slf4zio = "com.github.mlangc" %% "slf4zio" % "1.0.0"
  }

  object circe {
    private val version = "0.14.1"
    val core            = "io.circe" %% "circe-core"    % version
    val generic         = "io.circe" %% "circe-generic" % version
    val jawn            = "io.circe" %% "circe-jawn"    % version
  }

  object kafkaSerdeScala {
    private val version = "0.6.5"
    val circe           = "io.github.azhur" %% "kafka-serde-circe" % version

  }

  object confluent {
    val jsonSchemaSerializer = "io.confluent" % "kafka-json-schema-serializer" % "7.0.1"
    val schemaRegistryClient = "io.confluent" % "kafka-schema-registry-client" % "5.3.0"
  }

  object kamon {
    private val version  = "2.2.2"
    val bundle           = "io.kamon"                           %% "kamon-bundle"          % version
    val newRelic         = "io.kamon"                           %% "kamon-newrelic"        % version
    val newRelicAppender = "com.williamhill.platform.libraries" %% "newrelic-log-appender" % "0.1.4"
  }

  object williamHill {
    val facetEvent = "com.williamhill.platform" %% "facet-event-model" % "0.9.4"

    val kafkaLibraryCore = "com.williamhill.bettingengine" %% "be-kafka-library-core" % "1.2.8" excludeAll (ExclusionRule(
      "com.typesafe.akka",
      "akka-stream-kafka",
    ))
    val kafkaExtensions = "com.williamhill.platform" %% "zio-kafka-extensions" % "0.4.0-SNAPSHOT"

    object healthcheck {
      private val version = "0.1.10"
      val http4s          = "com.williamhill.platform" %% "http4s-healthcheck" % version
      val kafka           = "com.williamhill.platform" %% "kafka-healthcheck"  % version
    }

    object tracing {
      private val version = "0.1.1"
      val tracing         = "com.williamhill.platform.tracing" %% "zio-tracing"       % version
      val tracingKafka    = "com.williamhill.platform.tracing" %% "zio-tracing-kafka" % version
    }

  }
  object andyglow {
    private val version = "0.7.8"
    val core            = "com.github.andyglow" %% "scala-jsonschema-core"       % version
    val circeJson       = "com.github.andyglow" %% "scala-jsonschema-circe-json" % version
    val jsonSchemaCats  = "com.github.andyglow" %% "scala-jsonschema-cats"       % version
  }

  object enumeratum {
    private val version = "1.7.0"
    val circe           = "com.beachape" %% "enumeratum-circe" % version,
  }

  object http4s {
    private val version = "0.23.7"
    val dsl             = "org.http4s" %% "http4s-dsl" % version
  }
  object logback {
    val logstashLogbackEncoder = "net.logstash.logback" % "logstash-logback-encoder" % "7.2"
  }
  object testContainers {
    private val version = "0.40.7"
    val scalaKafka      = "com.dimafeng" %% "testcontainers-scala-kafka" % version
  }

  object monocle {
    private val version = "3.1.0"
    val core            = "dev.optics" %% s"monocle-core"  % version
    val `macro`         = "dev.optics" %% s"monocle-macro" % version

    // macro required for 2.x only
  }

}
