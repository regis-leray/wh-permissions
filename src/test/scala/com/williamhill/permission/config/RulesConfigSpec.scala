package com.williamhill.permission.config

import com.williamhill.permission.application.config.RulesConfig
import zio.ZIO
import zio.test.{DefaultRunnableSpec, ZSpec, assertCompletes}

object RulesConfigSpec extends DefaultRunnableSpec {

  def spec: ZSpec[Environment, Failure] = suite("RulesConfigSpec") {
    testM("Rules config can be loaded") {
      ZIO.service[RulesConfig].as(assertCompletes)
    }
  }.provideLayer(RulesConfig.live.orDie)
}
