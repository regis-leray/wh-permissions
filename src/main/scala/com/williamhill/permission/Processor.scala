package com.williamhill.permission

import cats.implicits.catsSyntaxEitherId
import zio.*
import zio.kafka.consumer.{Consumer, Subscription}
import zio.kafka.serde.Serde
import zio.stream.*
import com.github.mlangc.slf4zio.api.{Logging, logging as Log}
import com.williamhill.permission.application.{AppError, Env}
import com.williamhill.permission.application.config.ActionsConfig
import com.williamhill.permission.domain.FacetContext
import com.williamhill.permission.kafka.{EventPublisher, HasKey}
import com.williamhill.permission.kafka.Record.{FacetContextCommittable, InputRecord, OutputCommittable}
import com.williamhill.permission.kafka.events.generic.{InputEvent, OutputEvent}
import com.williamhill.platform.kafka as Kafka
import com.williamhill.platform.kafka.JsonSerialization
import com.williamhill.platform.kafka.config.{CommaSeparatedList, TopicConfig}
import com.williamhill.platform.kafka.consumer.Committable
import com.williamhill.platform.library.kafka.TracingConsumer
import pureconfig.ConfigReader
import pureconfig.generic.semiauto.deriveReader
import zio.clock.Clock

object Processor {
  // TODO: should we move all configs in application.config package?
  final case class Config(
      selfExclusionEvents: TopicConfig,
      configuredUniverses: CommaSeparatedList,
      playerFacetEvents: TopicConfig,
      tracingIdentifiers: Config.TracingIdentifiers,
  )
  object Config {
    final case class TracingIdentifiers(groupId: String, clientId: String)
    object TracingIdentifiers {
      implicit val reader: ConfigReader[TracingIdentifiers] = deriveReader
    }
    implicit val reader: ConfigReader[Config] = deriveReader
  }

  val selfExclusionEventsSource: ZStream[Has[Config] & Has[Consumer], AppError, InputRecord] =
    ZStream
      .service[Config]
      .flatMap { cfg =>
        ZStream
          .fromEffect(
            JsonSerialization.valueDeserializer[InputEvent](cfg.selfExclusionEvents.schemaRegistrySettings),
          )
          .flatMap { valueDes =>
            Consumer
              .subscribeAnd(Subscription.topics(cfg.selfExclusionEvents.topics.head, cfg.selfExclusionEvents.topics.tail*))
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
        universe            <- ZIO.fromEither(InputParser.parseUniverse(event))
        maybeSupportedEvent <- ZIO.service[Config].map(_.configuredUniverses.unwrap.find(_ == universe.value).map(_ => event))
        _                   <- ZIO.when(maybeSupportedEvent.isEmpty)(event.offset.commit).mapError(AppError.fromThrowable)
      } yield maybeSupportedEvent
    }

  val collectExclusionContext: ZTransducer[Any, AppError, InputRecord, FacetContextCommittable] =
    Committable
      .filterMapCommittableRecordM((event: InputRecord) => ZIO.succeed(InputParser.parse(event)))
      .mapError(AppError.fromThrowable)

  val calculatePermissions: ZTransducer[Has[ActionsConfig] & Clock, AppError, FacetContextCommittable, FacetContextCommittable] =
    Committable
      .mapValueThrowable(PermissionsLogic.enrichWithActions)
      .mapError(AppError.fromThrowable)

  val convertToFacetEvent: ZTransducer[ZEnv, AppError, FacetContextCommittable, OutputCommittable] =
    Committable
      .mapValueThrowable((facetContext: FacetContext) => ZIO.succeed(OutputEvent(facetContext)))
      .mapError(AppError.fromThrowable)

  val publishEvent: ZTransducer[Has[EventPublisher] & Has[Config], AppError, OutputCommittable, OutputCommittable] = {
    implicit val facetKey: HasKey[OutputEvent] = (_: OutputEvent).body.newValues.id
    Committable
      .mapValueM((event: OutputEvent) =>
        for {
          publisher <- ZIO.service[EventPublisher]
          cfg       <- ZIO.service[Config]
          _         <- publisher.publish(cfg.playerFacetEvents.topics.head, event)
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
    filterEventsFromSupportedUniverses >>> collectExclusionContext >>> calculatePermissions >>> convertToFacetEvent >>> publishEvent >>> commit

  val run: ZIO[Env.Main, AppError, Unit] =
    selfExclusionEventsSource >>> pipeline
}
