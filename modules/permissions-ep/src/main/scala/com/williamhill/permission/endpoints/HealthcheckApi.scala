package com.williamhill.permission.endpoints

import com.github.mlangc.slf4zio.api.{Logging, logging => Log}
import com.williamhill.permission.config.HealthcheckConfig
import com.williamhill.platform.healthcheck.OkHealthCheck
import com.williamhill.platform.healthcheck.http4s.server.HealthCheckRoutes
import com.williamhill.platform.healthcheck.kafka.KafkaHealthCheck
import org.http4s.HttpRoutes
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.{Router, Server}
import zio.blocking.Blocking
import zio.clock.Clock
import zio.duration.Duration
import zio.interop.catz.*
import zio.{Cause, RManaged}

object HealthcheckApi extends org.http4s.dsl.Http4sDsl[Effect] {
  def routes(config: HealthcheckConfig): HttpRoutes[Effect] =
    new HealthCheckRoutes(
      config.identifier,
      new OkHealthCheck(config.identifier),
      KafkaHealthCheck.withDefaults(config.bootstrapServers.toString(), Duration.fromScala(config.maxBlockTimeout)),
    ).routes

  def asResource(cfg: HealthcheckConfig): RManaged[Blocking & Clock & Logging, Server] =
    BlazeServerBuilder[Effect]
      .bindHttp(cfg.port, cfg.host)
      .withHttpApp(Router("/" -> routes(cfg)).orNotFound)
      .resource
      .toManagedZIO
      .tapBoth(
        ex => Log.errorIO(s"failed to start ${Cause.fail(ex).prettyPrint}").toManaged_,
        server =>
          Log
            .infoIO(
              s"healthcheck API started for ${cfg.identifier} on ${server.address.getHostString}:${server.address.getPort}",
            )
            .toManaged_,
      )
      .onExit(_ => Log.infoIO(s"healthcheck API stopped for ${cfg.identifier}"))
}
