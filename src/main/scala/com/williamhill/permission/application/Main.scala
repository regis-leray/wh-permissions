package com.williamhill.permission.application

import zio.*

import com.github.mlangc.slf4zio.api.logging as Log
import com.williamhill.permission.Processor

object Main extends App {
  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    Kamon.asResource
      .use_(
        Log.warnIO("commence consumption of self-exclusion events") *>
          Processor.run.mapError(appError=>appError.toThrowable)
            .tapBoth(
              Log.errorIO("stopped consumption of self-exclusion events, faiiled with error", _),
              _ => Log.warnIO("stopped consumption of self-exclusion events"),
            )
            .as(ExitCode.success),
      )

      .provideSomeLayer[ZEnv](Env.layer)
      .orDie
}
