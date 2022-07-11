package com.williamhill.permission.config

import com.williamhill.platform.kafka.config.{CommaSeparatedList, TopicConfig}
import pureconfig.ConfigReader
import pureconfig.generic.semiauto.deriveReader

final case class ProcessorConfig(
    inputEvents: TopicConfig,
    outputEvents: TopicConfig,
    configuredUniverses: CommaSeparatedList,
    tracingIdentifiers: ProcessorConfig.TracingIdentifiers,
)

object ProcessorConfig {
  implicit val reader: ConfigReader[ProcessorConfig] = deriveReader

  final case class TracingIdentifiers(groupId: String, clientId: String)
  implicit val tracingIdentifiersReader: ConfigReader[TracingIdentifiers] = deriveReader
}
