package com.williamhill.permission.config

import pureconfig.generic.semiauto.deriveReader
import pureconfig.{ConfigReader, ConfigSource}
import zio.blocking.{Blocking, blocking}
import zio.kafka.consumer.ConsumerSettings
import zio.kafka.producer.ProducerSettings
import zio.{Has, RLayer, ZIO}

final case class AppConfig(
    healthcheck: HealthcheckConfig,
    consumerSettings: ConsumerSettings,
    producerSettings: ProducerSettings,
    processorSettings: ProcessorConfig,
)

object AppConfig {
  implicit val reader1: ConfigReader[ProducerSettings] = com.williamhill.platform.kafka.producer.settings
  implicit val reader2: ConfigReader[ConsumerSettings] = com.williamhill.platform.kafka.consumer.settings

  implicit val reader: ConfigReader[AppConfig] = deriveReader[AppConfig]

  val live: RLayer[Blocking, Has[AppConfig]] =
    blocking(ZIO.effect(ConfigSource.default.loadOrThrow[AppConfig])).toLayer
}
