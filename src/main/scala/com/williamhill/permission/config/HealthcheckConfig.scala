package com.williamhill.permission.config

import com.williamhill.platform.kafka.config.CommaSeparatedList
import pureconfig.ConfigReader
import pureconfig.generic.semiauto.deriveReader

final case class HealthcheckConfig(
    identifier: String,
    host: String,
    port: Int,
    bootstrapServers: CommaSeparatedList,
    maxBlockTimeout: scala.concurrent.duration.FiniteDuration,
)

object HealthcheckConfig {
  implicit val reader: ConfigReader[HealthcheckConfig] = deriveReader[HealthcheckConfig]
}
