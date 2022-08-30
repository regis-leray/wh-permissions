package com.williamhill.permission

import com.github.mlangc.slf4zio.api.{Logging, logging as Log}
import com.williamhill.permission.application.Env
import com.williamhill.permission.application.Env.Processor
import com.williamhill.permission.config.AppConfig
import com.williamhill.permission.endpoints.HealthcheckApi
import org.http4s.server.Server
import zio.*
import zio.blocking.Blocking
import zio.clock.Clock
import zio.console.Console
import zio.magic.*

object PermissionEp extends App {

  object Kamon {
    val asResource: TaskManaged[kamon.Kamon.type] =
      ZManaged.make(ZIO.effect { kamon.Kamon.init(); kamon.Kamon })(kamon => ZIO.fromFuture(_ => kamon.stop()).orDie)
  }

  val server: ZManaged[Blocking & Clock & Logging & Has[AppConfig], Throwable, Server] =
    ZManaged.service[AppConfig].flatMap(cfg => HealthcheckApi.asResource(cfg.healthcheck))

  val ep: ZManaged[Processor & Console, Throwable, Unit] = Kamon.asResource
    .use_(
      Log.warnIO("START consuming input events") *>
        Processor.run
          .mapError(_.toThrowable)
          .tapBoth(
            Log.errorIO("STOP consuming input events => failed with error", _),
            _ => Log.warnIO("STOP consuming input events"),
          )
          .onExit(exit => Log.infoIO(s"Shutting down input events processor: $exit")),
    )
    .toManaged_

  override def run(args: List[String]): URIO[ZEnv, ExitCode] =
    (server <*> ep.fork).useForever.injectSome[ZEnv](Env.layer).exitCode
}
