package com.williamhill.permission.application

import com.github.mlangc.slf4zio.api.Logging
import com.williamhill.permission.Processor.Config
import com.williamhill.permission.application.config.{ActionsConfig, AppConfig, MappingsConfig}
import com.williamhill.permission.kafka.{EventPublisher, EventPublisherLive}
import com.williamhill.permission.{EventProcessor, FacetContextParser, PermissionLogic}
import org.http4s.server.Server
import zio.*
import zio.kafka.consumer.Consumer
import zio.kafka.producer.Producer
import zio.magic.*

object Env {

  type Processor = Has[Config] &
    Has[EventPublisher] &
    Has[EventProcessor] &
    ZEnv &
    Logging

  type Main = Has[Consumer] &
    Has[Producer] &
    Has[Server] &
    Processor

  val layer: RLayer[ZEnv, Main] =
    ZLayer.wireSome[ZEnv, Main](
      Logging.global,
      AppConfig.layer,
      ActionsConfig.layer,
      MappingsConfig.layer,
      FacetContextParser.layer,
      PermissionLogic.layer,
      EventProcessor.layer,
      ZManaged.service[AppConfig].flatMap(cfg => Consumer.make(cfg.consumerSettings)).toLayer,
      ZManaged.service[AppConfig].flatMap(cfg => Producer.make(cfg.producerSettings)).toLayer,
      ZManaged.service[AppConfig].flatMap(cfg => HealthcheckApi.asResource(cfg.healthcheck)).toLayer,
      ZIO.service[AppConfig].map(cfg => cfg.processorSettings).toLayer,
      EventPublisherLive.layer,
    )
}
