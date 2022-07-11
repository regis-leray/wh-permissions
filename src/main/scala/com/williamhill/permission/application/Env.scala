package com.williamhill.permission.application

import com.github.mlangc.slf4zio.api.Logging
import com.williamhill.permission.config.{AppConfig, MappingsConfig, ProcessorConfig, RulesConfig}
import com.williamhill.permission.kafka.EventPublisher
import com.williamhill.permission.kafka.serde.AppSerdes
import com.williamhill.permission.{EventProcessor, FacetContextParser, PermissionLogic}
import com.williamhill.platform.event.permission.Event as OutputEvent
import org.http4s.server.Server
import zio.*
import zio.blocking.Blocking
import zio.clock.Clock
import zio.kafka.consumer.Consumer
import zio.kafka.consumer.diagnostics.Diagnostics
import zio.kafka.producer.Producer
import zio.kafka.serde.Serde as ZioSerde
import zio.magic.*

object Env {

  type Processor = Has[ProcessorConfig] &
    Has[EventPublisher] &
    Has[EventProcessor] &
    Clock & Blocking &
    Logging & Has[AppConfig] & Has[Consumer] & Has[ZioSerde[Any, OutputEvent]]

  type Main =
    Has[Server] &
      Processor

  val configLayer = AppConfig.live ++ RulesConfig.live ++ MappingsConfig.live

  val diagnosticsNoopLayer: ULayer[Has[Diagnostics]] = ZLayer.succeed[Diagnostics](Diagnostics.NoOp)

  /// TODO move inside PermissionEp
  val serverLayer: ZLayer[Blocking & Clock & Logging & Has[AppConfig], Throwable, Has[Server]] =
    ZManaged.service[AppConfig].flatMap(cfg => HealthcheckApi.asResource(cfg.healthcheck)).toLayer

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

  val layer: RLayer[Clock & Blocking, Main] =
    ZLayer.wireSome[Clock & Blocking, Main](
      Logging.global,
      AppConfig.live,
      RulesConfig.live,
      MappingsConfig.live,
      FacetContextParser.layer,
      PermissionLogic.layer,
      EventProcessor.layer,
      AppSerdes.outputSerdeLayer,
      consumerLayer,
      producerLayer,
      serverLayer,
      ZIO.service[AppConfig].map(cfg => cfg.processorSettings).toLayer,
      EventPublisher.layer,
    )
}
