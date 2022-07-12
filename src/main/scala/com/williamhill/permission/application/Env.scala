package com.williamhill.permission.application

import com.github.mlangc.slf4zio.api.Logging
import com.williamhill.permission.config.{AppConfig, MappingsConfig, RulesConfig}
import com.williamhill.permission.kafka.EventPublisher
import com.williamhill.permission.kafka.serde.AppSerdes
import com.williamhill.permission.{EventProcessor, FacetContextParser, PermissionLogic}
import com.williamhill.platform.event.permission.Event as OutputEvent
import zio.*
import zio.blocking.Blocking
import zio.clock.Clock
import zio.kafka.consumer.Consumer
import zio.kafka.consumer.diagnostics.Diagnostics
import zio.kafka.producer.Producer
import zio.kafka.serde.Serde as ZioSerde
import zio.magic.*

object Env {

  val diagnosticsNoopLayer: ULayer[Has[Diagnostics]] = ZLayer.succeed[Diagnostics](Diagnostics.NoOp)

  // TODO move in kafka layer
  val consumerLayer: ZLayer[Clock & Blocking & Has[AppConfig], Throwable, Has[Consumer]] = {
    val consumerManaged: ZManaged[Clock & Blocking & Has[AppConfig], Throwable, Consumer] = for {
      cfg    <- ZManaged.service[AppConfig]
      result <- Consumer.make(cfg.consumerSettings)
    } yield result
    consumerManaged.toLayer
  }

  // TODO move in kafka layer
  val producerLayer: RLayer[Blocking & Has[AppConfig], Has[Producer]] =
    ZManaged.service[AppConfig].flatMap(cfg => Producer.make(cfg.producerSettings)).toLayer

  type Config =
    Has[AppConfig] & Has[RulesConfig] & Has[MappingsConfig]

  val config: ZLayer[Blocking, Throwable, Config] =
    AppConfig.live ++
      RulesConfig.live ++
      MappingsConfig.live

  type Core =
    Logging &
      Has[EventPublisher] &
      Has[EventProcessor] &
      Has[Consumer] &
      Has[ZioSerde[Any, OutputEvent]]

  val core = ZLayer.wireSome[Clock & Blocking & Config, Core](
    Logging.global,
    FacetContextParser.layer,
    PermissionLogic.layer,
    EventProcessor.layer,
    AppSerdes.outputSerdeLayer,
    consumerLayer,
    producerLayer,
    EventPublisher.layer,
  )

  type Processor =
    Core & Config

  val layer: RLayer[Clock & Blocking, Processor] =
    ZLayer.wireSome[Clock & Blocking, Processor](config, core)
}
