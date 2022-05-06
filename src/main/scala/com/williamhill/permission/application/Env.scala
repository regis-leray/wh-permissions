package com.williamhill.permission.application

import zio.*
import zio.kafka.consumer.{Consumer, ConsumerSettings}
import zio.kafka.producer.{Producer, ProducerSettings}
import zio.magic.*

import com.github.mlangc.slf4zio.api.Logging
import com.williamhill.permission.kafka.EventPublisherLive
import com.williamhill.permission.{Env, Processor}
import com.williamhill.platform.kafka.consumer.settings as cSettings
import com.williamhill.platform.kafka.producer.settings as pSettings

import pureconfig.generic.semiauto.deriveReader
import pureconfig.{ConfigReader, ConfigSource}

object Env {
  final case class Config(
      healthcheck: HealthcheckApi.Config,
      consumerSettings: ConsumerSettings,
      producerSettings: ProducerSettings,
      processorSettings: Processor.Config
  )
  object Config {
    implicitly[ConfigReader[ConsumerSettings]]
    implicitly[ConfigReader[ProducerSettings]]

    implicit val reader: ConfigReader[Config] =
      deriveReader[Config]

    val load: Task[Config] = Task { ConfigSource.default.loadOrThrow[Config] }
  }

  val layer: RLayer[ZEnv, Env] =
    ZLayer.wireSome[ZEnv, Env](
      Logging.global,
      Config.load.toManaged_.toLayer,
      ZManaged.service[Config].flatMap { cfg => Consumer.make(cfg.consumerSettings) }.toLayer,
      ZManaged.service[Config].flatMap { cfg => Producer.make(cfg.producerSettings) }.toLayer,
      ZManaged.service[Config].flatMap { cfg => HealthcheckApi.asResource(cfg.healthcheck) }.toLayer,
      ZIO.service[Config].map { cfg => cfg.processorSettings }.toLayer,
      EventPublisherLive.layer
    )
}
