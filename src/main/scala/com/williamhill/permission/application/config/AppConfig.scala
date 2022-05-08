package com.williamhill.permission.application.config

import com.williamhill.permission.Processor
import com.williamhill.permission.application.HealthcheckApi
import pureconfig.{ConfigReader, ConfigSource}
import pureconfig.generic.semiauto.deriveReader
import zio.{Has, RLayer, ZIO}
import zio.blocking.{Blocking, blocking}
import zio.kafka.consumer.ConsumerSettings
import zio.kafka.producer.ProducerSettings
import com.williamhill.platform.kafka.consumer.settings as cSettings
import com.williamhill.platform.kafka.producer.settings as pSettings

final case class AppConfig(
    healthcheck: HealthcheckApi.Config,
    consumerSettings: ConsumerSettings,
    producerSettings: ProducerSettings,
    processorSettings: Processor.Config,
)

object AppConfig {
  implicitly[ConfigReader[ConsumerSettings]]
  implicitly[ConfigReader[ProducerSettings]]

  implicit val reader: ConfigReader[AppConfig] = deriveReader[AppConfig]

  val layer: RLayer[Blocking, Has[AppConfig]] = blocking(ZIO.effect(ConfigSource.default.loadOrThrow[AppConfig])).orDie.toLayer
}
