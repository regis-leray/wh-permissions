package com.williamhill.permission.application

import com.github.mlangc.slf4zio.api.Logging
import com.williamhill.permission.Processor.Config
import com.williamhill.permission.application.config.{AppConfig, MappingsConfig, RulesConfig}
import com.williamhill.permission.kafka.EventPublisher
import com.williamhill.permission.kafka.events.generic.OutputEvent
import com.williamhill.permission.kafka.serde.AppSerdes
import com.williamhill.permission.{EventProcessor, FacetContextParser, PermissionLogic}
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

  type Processor = Has[Config] &
    Has[EventPublisher] &
    Has[EventProcessor] &
    ZEnv &
    Logging & Has[AppConfig] & Has[Consumer] & Has[ZioSerde[Any, OutputEvent]]

  type Main =
    Has[Server] &
      Processor

  val diagnosticsNoopLayer: ULayer[Has[Diagnostics]] = ZLayer.succeed[Diagnostics](Diagnostics.NoOp)

  val serverLayer: ZLayer[Blocking & Clock & Logging & Has[AppConfig], Throwable, Has[Server]] =
    ZManaged.service[AppConfig].flatMap(cfg => HealthcheckApi.asResource(cfg.healthcheck)).toLayer

  val consumerLayer: ZLayer[Clock & Blocking & Has[AppConfig], Throwable, Has[Consumer]] = {
    val consumerManaged: ZManaged[Clock & Blocking & Has[AppConfig], Throwable, Consumer] = for {
      cfg    <- ZManaged.service[AppConfig]
      result <- Consumer.make(cfg.consumerSettings)
    } yield result
    consumerManaged.toLayer
  }
  val producerLayer: RLayer[Blocking & Has[AppConfig], Has[Producer]] =
    ZManaged.service[AppConfig].flatMap(cfg => Producer.make(cfg.producerSettings)).toLayer

  val layer: RLayer[ZEnv, Main] =
    ZLayer.wireSome[ZEnv, Main](
      Logging.global,
      AppConfig.layer,
      RulesConfig.layer,
      MappingsConfig.layer,
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
