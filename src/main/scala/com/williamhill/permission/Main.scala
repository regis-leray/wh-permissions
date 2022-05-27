package com.williamhill.permission

import com.github.mlangc.slf4zio.api.logging as Log
import com.williamhill.permission.application.Env
import zio.{App, ExitCode, URIO, ZEnv}

object Main extends App {
  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    println(s"App starting ......")
//    Kamon.asResource
//      .use_(
//    Log.warnIO("START consuming input events") *>
    Processor.run
      .mapError(appError => appError.toThrowable)
      .tapBoth(
        Log.errorIO("STOP consuming input events => failed with error", _),
        _ => Log.warnIO("STOP consuming input events"),
      )
      .as(ExitCode.success)
//      )
      .provideSomeLayer[ZEnv](Env.layer)
      .orDie
  }
}
