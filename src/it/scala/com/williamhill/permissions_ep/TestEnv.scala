package com.williamhill.permissions_ep

import cats.data.NonEmptyList
import zio.blocking.{Blocking, blocking}
import zio.clock.Clock
import zio.kafka.consumer.Consumer.{AutoOffsetStrategy, OffsetRetrieval}
import zio.kafka.consumer.{Consumer, ConsumerSettings}
import zio.kafka.producer.{Producer, ProducerSettings}
import zio.magic.*
import zio.{Has, RLayer, Task, URLayer, ZEnv, ZIO, ZLayer}

import com.dimafeng.testcontainers.{KafkaContainer, SchemaRegistryContainer}
import com.github.mlangc.slf4zio.api.Logging
import com.williamhill.permission.Processor.Config
import com.williamhill.permission.application.Env
import com.williamhill.permission.application.config.{AppConfig, MappingsConfig, RulesConfig}
import com.williamhill.permission.kafka.EventPublisher
import com.williamhill.permission.kafka.events.generic.{InputEvent, OutputEvent}
import com.williamhill.permission.{EventProcessor, FacetContextParser, PermissionLogic, Processor}
import com.williamhill.permissions_ep.TestContainers.*
import com.williamhill.platform.kafka.config.CommaSeparatedList
import org.testcontainers.containers.Network
import org.testcontainers.utility.DockerImageName
import zio.kafka.serde.Serde as ZioSerde

import com.williamhill.permission.kafka.serde.AppSerdes

import monocle.syntax.all.*
import pureconfig.ConfigSource

object TestEnv {

  private val groupId = s"groupId-$kafkaBrokerId"

  private val networkLayer: URLayer[Blocking, Has[Network]] =
    ZLayer.fromAcquireRelease(createNetwork)(stopNetwork)

  type TestProcessor =
    Has[TestInputEventProducer] & Has[TestOutputEventConsumer] & Has[ConsumerSettings] & Has[ProducerSettings] & Env.Processor & Has[
      ZioSerde[Any, InputEvent],
    ]

  /** Services that need to be defined in test code
    */
  type OverriddenEnv =
    Has[Processor.Config] & Has[Producer] & Has[Consumer] & Has[ConsumerSettings] & Has[ProducerSettings] & Has[EventPublisher] & Has[
      AppConfig,
    ]

  /** Coming from production code
    */
  type PartialProcessor = Has[EventProcessor] & Logging & Has[ZioSerde[Any, OutputEvent]] & Has[ZioSerde[Any, InputEvent]]

  val kafkaContainerLayer: ZLayer[Blocking, Nothing, Has[KafkaContainer]] =
    ZLayer.wireSome[Blocking, Has[KafkaContainer]](
      networkLayer,
      ZLayer.fromAcquireRelease(
        for {
          kafkaContainer <- startKafkaContainer(KafkaContainer(DockerImageName.parse(defaultDockerImageName)))
        } yield kafkaContainer,
      )(
        stopKafkaContainer,
      ),
    )

  val schemaRegistryLayer: ZLayer[Blocking, Nothing, Has[SchemaRegistryContainer]] =
    ZLayer.wireSome[Blocking, Has[SchemaRegistryContainer]](
      networkLayer,
      ZLayer.fromAcquireRelease(TestContainers.startSchemaRegistryContainer)(TestContainers.stopSchemaRegistryContainer),
    )

  val producerSettingsLayer: URLayer[Has[KafkaContainer], Has[ProducerSettings]] =
    ZLayer.fromEffect(makeSettings.map(ProducerSettings.apply))

  val consumerSettingsLayer: URLayer[Has[KafkaContainer], Has[ConsumerSettings]] =
    ZLayer.fromEffect(
      makeSettings.map(bootstrapServers =>
        ConsumerSettings(bootstrapServers).withGroupId(groupId).copy(offsetRetrieval = OffsetRetrieval.Auto(AutoOffsetStrategy.Earliest)),
      ),
    )

  val appConfigLayer: URLayer[Blocking & Has[ConsumerSettings] & Has[ProducerSettings] & Has[Processor.Config], Has[AppConfig]] = {

    (for {
      consumerSettings <- ZIO.service[ConsumerSettings]
      producerSettings <- ZIO.service[ProducerSettings]
      processorConfig  <- ZIO.service[Processor.Config]
      appConfig        <- blocking(ZIO.effect(ConfigSource.default.loadOrThrow[AppConfig])).orDie
      result = appConfig.copy(
        consumerSettings = consumerSettings,
        producerSettings = producerSettings,
        healthcheck = appConfig.healthcheck.copy(bootstrapServers =
          NonEmptyList.fromList(consumerSettings.bootstrapServers).fold(appConfig.healthcheck.bootstrapServers)(new CommaSeparatedList(_)),
        ),
        processorSettings = processorConfig,
      )
    } yield result).toLayer

  }

  val processorConfigLayer: URLayer[Has[SchemaRegistryContainer], Has[Processor.Config]] = ZLayer.fromEffect {
    for {
      envConfig      <- Task.effect(ConfigSource.resources("application-test.conf").loadOrThrow[AppConfig]).orDie
      schemaRegistry <- ZIO.service[SchemaRegistryContainer]
      config = envConfig.processorSettings
    } yield config
      .focus(_.inputEvents.schemaRegistrySettings.schemaRegistryUrl)
      .replace(schemaRegistry.schemaUrl)
      .focus(_.inputEvents.schemaRegistrySettings.underlyingConfig)
      .replace(None) // Just cleaning up underlying config, otherwise it is confusing when printing
      .focus(_.outputEvents.schemaRegistrySettings.schemaRegistryUrl)
      .replace(schemaRegistry.schemaUrl)
      .focus(_.outputEvents.schemaRegistrySettings.underlyingConfig)
      .replace(None) // Just cleaning up underlyuing config, otherwise it is confusing when printing
  }

  private def makeSettings: ZIO[Has[KafkaContainer], Nothing, List[String]] =
    ZIO
      .service[KafkaContainer]
      .map { container =>
        val bootstrapServer: String = container.bootstrapServers.replace("PLAINTEXT://", "") // container.bootstrapServers //
        List(bootstrapServer)
      }

  val producerLayer: ZLayer[Blocking, Nothing, Has[Producer] & Has[ProducerSettings]] =
    ZLayer.wireSome[Blocking, Has[Producer] & Has[ProducerSettings]](
      kafkaContainerLayer,
      producerSettingsLayer,
      Producer.live.orDie,
    )

  val consumerLayer: URLayer[Blocking & Clock, Has[Consumer] & Has[ConsumerSettings]] =
    ZLayer.wireSome[Blocking & Clock, Has[Consumer] & Has[ConsumerSettings]](
      kafkaContainerLayer,
      consumerSettingsLayer,
      Env.diagnosticsNoopLayer,
      Consumer.live.orDie,
    )

  val partialProcessorLayer: RLayer[Blocking & Clock & Has[Config] & Has[Producer], PartialProcessor] =
    ZLayer.wireSome[Blocking & Clock & Has[Processor.Config] & Has[Producer], PartialProcessor](
      Logging.global,
      RulesConfig.layer,
      MappingsConfig.layer,
      FacetContextParser.layer,
      PermissionLogic.layer,
      EventProcessor.layer,
      AppSerdes.inputSerdeLayer,
      AppSerdes.outputSerdeLayerTest,
    )
  val overriddenEnv: URLayer[Blocking & Clock, OverriddenEnv] = ZLayer.wireSome[Blocking & Clock, OverriddenEnv](
    schemaRegistryLayer,
    processorConfigLayer,
    producerLayer,
    consumerLayer,
    appConfigLayer,
    EventPublisher.layer,
  )

  val testLayer: RLayer[ZEnv, TestProcessor] =
    ZLayer.wireSome[ZEnv, TestProcessor](
      overriddenEnv,
      TestInputEventProducer.layer,
      TestOutputEventConsumer.layer,
      partialProcessorLayer,
    )
}
