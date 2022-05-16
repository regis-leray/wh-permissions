package com.williamhill.permission

import com.github.mlangc.slf4zio.api.logging as Log
import com.williamhill.permission.application.{Env, Kamon}
import zio.{App, ExitCode, URIO, ZEnv}

object Main extends App {
  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    Kamon.asResource
      .use_(
        Log.warnIO("commence consumption of self-exclusion events") *>
          Processor.run
            .mapError(appError => appError.toThrowable)
            .tapBoth(
              Log.errorIO("stopped consumption of self-exclusion events, faiiled with error", _),
              _ => Log.warnIO("stopped consumption of self-exclusion events"),
            )
            .as(ExitCode.success),
      )
      .provideSomeLayer[ZEnv](Env.layer)
      .orDie
}
