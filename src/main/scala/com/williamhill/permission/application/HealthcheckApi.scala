package com.williamhill.permission.application

import com.github.mlangc.slf4zio.api.{Logging, logging => Log}
import com.williamhill.platform.healthcheck.OkHealthCheck
import com.williamhill.platform.healthcheck.http4s.server.HealthCheckRoutes
import com.williamhill.platform.healthcheck.kafka.KafkaHealthCheck
import com.williamhill.platform.kafka.config.CommaSeparatedList
import org.http4s.*
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.implicits.*
import org.http4s.server.{Router, Server}
import pureconfig.ConfigReader
import pureconfig.generic.semiauto.deriveReader
import zio.*
import zio.blocking.*
import zio.clock.*
import zio.duration.Duration
import zio.interop.catz.*

object HealthcheckApi extends org.http4s.dsl.Http4sDsl[RIO[Clock & Blocking, _]] {
  final case class Config(
      identifier: String,
      host: String,
      port: Int,
      bootstrapServers: CommaSeparatedList,
      maxBlockTimeout: scala.concurrent.duration.FiniteDuration,
  )
  object Config {
    implicit val reader: ConfigReader[Config] = deriveReader[Config]
  }

  def routes(config: Config): HttpRoutes[RIO[Clock & Blocking, _]] =
    new HealthCheckRoutes(
      config.identifier,
      new OkHealthCheck(config.identifier),
      KafkaHealthCheck.withDefaults(config.bootstrapServers.toString(), Duration.fromScala(config.maxBlockTimeout)),
    ).routes

  def asResource(cfg: Config): RManaged[Blocking & Clock & Logging, Server] =
    BlazeServerBuilder[RIO[Clock & Blocking, _]]
      .bindHttp(cfg.port, cfg.host)
      .withHttpApp(Router("/" -> routes(cfg)).orNotFound)
      .resource
      .toManagedZIO
      .tapBoth(
        ex => Log.errorIO(s"failed to start ${Cause.fail(ex).prettyPrint}").toManaged_,
        server =>
          Log
            .infoIO(
              s"healthcheck API started for ${cfg.identifier} on ${server.address.getHostString()}:${server.address.getPort()}",
            )
            .toManaged_,
      )
      .onExit(_ => Log.infoIO(s"healthcheck API stopped for ${cfg.identifier}"))
}
