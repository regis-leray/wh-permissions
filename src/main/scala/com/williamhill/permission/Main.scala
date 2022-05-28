package com.williamhill.permission

import com.github.mlangc.slf4zio.api.logging as Log
import com.williamhill.permission.application.Env
import zio.{App, ExitCode, URIO, ZEnv}

object Main extends App {
  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
//    Kamon.asResource
//      .use_(
//    Log.warnIO("START consuming input events") *>
    Processor.run
      .mapError(appError => appError.toThrowable)
      .tapBoth(
        Log.errorIO("STOP consuming input events => failed with error", _),
        _ => Log.warnIO("STOP consuming input events"),
      )
      .onExit(exit => Log.infoIO(s"Shutting down input events processor: $exit"))
      .exitCode
//      )
      .provideSomeLayer[ZEnv](Env.layer)
      .orDie
  }
}
