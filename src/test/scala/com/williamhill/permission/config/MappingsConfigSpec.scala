package com.williamhill.permission.config

import zio.ZIO
import zio.test.{DefaultRunnableSpec, ZSpec, assertCompletes}

object MappingsConfigSpec extends DefaultRunnableSpec {
  def spec: ZSpec[Environment, Failure] = suite("MappingsConfigSpec") {
    testM("Mappings config can be loaded") {
      ZIO.service[MappingsConfig].as(assertCompletes)
    }
  }.provideLayer(MappingsConfig.live.orDie)
}
