package com.williamhill.permission

import cats.implicits.catsSyntaxEitherId
import com.github.mlangc.slf4zio.api.{Logging, logging as Log}
import com.williamhill.permission.application.{AppError, Env}
import com.williamhill.permission.kafka.Record.{InputRecord, OutputCommittable}
import com.williamhill.permission.kafka.events.generic.{InputEvent, OutputEvent}
import com.williamhill.permission.kafka.{EventPublisher, HasKey}
import com.williamhill.platform.kafka.JsonSerialization
import com.williamhill.platform.kafka.config.{CommaSeparatedList, TopicConfig}
import com.williamhill.platform.kafka.consumer.Committable
import com.williamhill.platform.library.kafka.TracingConsumer
import com.williamhill.platform.kafka as Kafka
import pureconfig.ConfigReader
import pureconfig.generic.semiauto.deriveReader
import zio.*
import zio.clock.Clock
import zio.kafka.consumer.{CommittableRecord, Consumer, Subscription}
import zio.kafka.serde.Serde
import zio.stream.*

object Processor {
  // TODO: should we move all configs in application.config package?
  final case class Config(
      inputEvents: TopicConfig,
      outputEvents: TopicConfig,
      configuredUniverses: CommaSeparatedList,
      tracingIdentifiers: Config.TracingIdentifiers,
  )

  object Config {
    final case class TracingIdentifiers(groupId: String, clientId: String)
    implicit val tracingIdentifiersReader: ConfigReader[TracingIdentifiers] = deriveReader
    implicit val reader: ConfigReader[Config]                               = deriveReader
  }

  val inputRecordEvents: ZStream[Has[Config] & Has[Consumer], AppError, InputRecord] =
    ZStream
      .service[Config]
      .flatMap { cfg =>
        ZStream
          .fromEffect(
            JsonSerialization.valueDeserializer[InputEvent](cfg.inputEvents.schemaRegistrySettings),
          )
          .flatMap { valueDes =>
            Consumer
              .subscribeAnd(Subscription.topics(cfg.inputEvents.topics.head, cfg.inputEvents.topics.tail*))
              .plainStream(Serde.string, valueDes)
              .tap { rec =>
                Task {
                  TracingConsumer.runWithConsumerSpanWithKamonHeader(
                    cfg.tracingIdentifiers.groupId,
                    cfg.tracingIdentifiers.clientId,
                    rec.record,
                  )(rec)
                }
              }
          }
          .mapError(AppError.fromThrowable)
      }

  val filterEventsFromSupportedUniverses: ZTransducer[Has[Config] & Clock, AppError, InputRecord, InputRecord] =
    Kafka.filterMapM { event =>
      for {
        maybeSupportedEvent <- ZIO.service[Config].map(_.configuredUniverses.unwrap.find(_ == event.value.header.universe).map(_ => event))
        _                   <- ZIO.when(maybeSupportedEvent.isEmpty)(event.offset.commit).mapError(AppError.fromThrowable)
      } yield maybeSupportedEvent
    }

  val inputToOutput: ZTransducer[Has[EventProcessor], AppError, CommittableRecord[String, InputEvent], OutputCommittable] =
    Committable
      .filterMapCommittableRecordM((inputRecord: InputRecord) =>
        for {
          outputEvent <- EventProcessor.handleInput(inputRecord.value).either
          _           <- outputEvent.fold(error => ZIO.debug(error), _ => ZIO.unit)
        } yield outputEvent.toOption,
      )
      .mapError(AppError.fromThrowable)

  val publishEvent: ZTransducer[Has[EventPublisher] & Has[Config], AppError, OutputCommittable, OutputCommittable] = {
    implicit val facetKey: HasKey[OutputEvent] = (_: OutputEvent).body.newValues.id
    Committable
      .mapValueM((event: OutputEvent) =>
        for {
          publisher <- ZIO.service[EventPublisher]
          cfg       <- ZIO.service[Config]
          _         <- publisher.publish(cfg.outputEvents.topics.head, event)
        } yield event.asRight[AppError],
      )
      .mapError(AppError.fromThrowable)
  }

  val commit: ZSink[Logging, AppError, OutputCommittable, OutputCommittable, Unit] =
    ZSink
      .foreach { record: OutputCommittable =>
        record.value.fold(
          err => Log.errorIO(s"failed to process event, result: $err"),
          x => Log.debugIO(s"successfully read: $x"),
        ) *> record.offset.commit
      }
      .mapError(AppError.fromThrowable)

  val pipeline: ZSink[Env.Processor, AppError, InputRecord, OutputCommittable, Unit] =
    filterEventsFromSupportedUniverses >>> inputToOutput >>> publishEvent >>> commit

  val run: ZIO[Env.Main, AppError, Unit] =
    inputRecordEvents >>> pipeline
}
