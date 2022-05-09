package com.williamhill.permission.application

import com.github.mlangc.slf4zio.api.Logging
import com.williamhill.permission.Processor
import com.williamhill.permission.application.config.{ActionsConfig, AppConfig}
import com.williamhill.permission.kafka.{EventPublisher, EventPublisherLive}
import org.http4s.server.Server
import zio.*
import zio.kafka.consumer.Consumer
import zio.kafka.producer.Producer
import zio.magic.*

object Env {

  type Main = Has[Consumer] &
    Has[Producer] &
    Has[Server] &
    Has[Processor.Config] &
    Has[EventPublisher] &
    Has[ActionsConfig] &
    ZEnv &
    Logging

  type Processor = Has[Processor.Config] &
    Has[EventPublisher] &
    Has[ActionsConfig] &
    ZEnv &
    Logging

  val layer: RLayer[ZEnv, Main] =
    ZLayer.wireSome[ZEnv, Main](
      Logging.global,
      AppConfig.layer,
      ActionsConfig.layer,
      ZManaged.service[AppConfig].flatMap(cfg => Consumer.make(cfg.consumerSettings)).toLayer,
      ZManaged.service[AppConfig].flatMap(cfg => Producer.make(cfg.producerSettings)).toLayer,
      ZManaged.service[AppConfig].flatMap(cfg => HealthcheckApi.asResource(cfg.healthcheck)).toLayer,
      ZIO.service[AppConfig].map(cfg => cfg.processorSettings).toLayer,
      EventPublisherLive.layer,
    )
}
