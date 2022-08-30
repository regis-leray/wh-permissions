package com.williamhill.permission.config

import zio.ZIO
import zio.test.{DefaultRunnableSpec, ZSpec, assertCompletes}

object AppConfigSpec extends DefaultRunnableSpec {
  def spec: ZSpec[Environment, Failure] = suite("AppConfigSpec") {
    testM("App config can be loaded") {
      ZIO.service[AppConfig].as(assertCompletes)
    }
  }.provideLayer(AppConfig.live.orDie)
}
