package com.williamhill.permission.application.config

import com.williamhill.permission.Processor
import com.williamhill.permission.application.HealthcheckApi
import com.williamhill.platform.kafka.consumer.settings as cSettings
import com.williamhill.platform.kafka.producer.settings as pSettings
import monocle.syntax.all.*
import pureconfig.generic.semiauto.deriveReader
import pureconfig.{ConfigReader, ConfigSource}
import zio.blocking.{Blocking, blocking}
import zio.kafka.consumer.Consumer.{AutoOffsetStrategy, OffsetRetrieval}
import zio.kafka.consumer.ConsumerSettings
import zio.kafka.producer.ProducerSettings
import zio.{Has, RLayer, ZIO}

final case class AppConfig(
    healthcheck: HealthcheckApi.Config,
    consumerSettings: ConsumerSettings,
    producerSettings: ProducerSettings,
    processorSettings: Processor.Config,
)

object AppConfig {

  // TODO add a test, I suspect that offset-retrival might not take the value from config
  implicit val reader: ConfigReader[AppConfig] = deriveReader[AppConfig]

  val layer: RLayer[Blocking, Has[AppConfig]] =
    blocking(
      ZIO
        .effect(ConfigSource.default.loadOrThrow[AppConfig])
        .map(appConfig =>
          appConfig
            .focus(_.consumerSettings.offsetRetrieval)
            .replace(OffsetRetrieval.Auto(AutoOffsetStrategy.Earliest)),
        ),
    ).orDie.toLayer

}
