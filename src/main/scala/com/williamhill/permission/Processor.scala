package com.williamhill.permission

import cats.implicits.catsSyntaxEitherId
import com.github.mlangc.slf4zio.api.{Logging, logging as Log}
import com.williamhill.permission.application.{AppError, Env}
import com.williamhill.permission.config.AppConfig
import com.williamhill.permission.kafka.Record.{OutputCommittable, StringRecord}
import com.williamhill.permission.kafka.events.generic.InputEvent
import com.williamhill.permission.kafka.{EventPublisher, HasKey}
import com.williamhill.platform.event.permission.Event as OutputEvent
import com.williamhill.platform.kafka.consumer.Committable
import com.williamhill.platform.library.kafka.TracingConsumer
import zio.*
import zio.kafka.consumer.*
import zio.kafka.serde.Serde as ZioSerde
import zio.stream.*

object Processor {

  val inputRecordEvents: ZStream[Has[Consumer] & Has[AppConfig], AppError, StringRecord] = {

    (for {
      config <- ZStream.service[AppConfig].map(_.processorSettings)
      subscription = Subscription.topics(config.inputEvents.topics.head, config.inputEvents.topics.tail*)
      rec <- Consumer
        .subscribeAnd(subscription)
        // TODO change serdes with InputEvent
        .plainStream(ZioSerde.string, ZioSerde.string)
        .tap { rec =>
          Task {
            TracingConsumer.runWithConsumerSpanWithKamonHeader(
              config.tracingIdentifiers.groupId,
              config.tracingIdentifiers.clientId,
              rec.record,
            )(rec)
          }
        }
    } yield rec).mapError(AppError.fromThrowable)

  }

  val inputToOutput: ZTransducer[Has[EventProcessor] & Has[AppConfig] & Logging, AppError, StringRecord, OutputCommittable] =
    Committable
      .filterMapCommittableRecordM { (inputRecord: StringRecord) =>
        val result: ZIO[Has[EventProcessor] & Has[AppConfig], AppError, OutputEvent] = for {
          config     <- ZIO.service[AppConfig].map(_.processorSettings)
          inputEvent <- ZIO.fromEither(InputEvent.produce(inputRecord.record.value()))
          universe            = inputEvent.header.universe
          maybeSupportedEvent = config.configuredUniverses.unwrap.find(_ == inputEvent.header.universe).map(_ => inputEvent)
          _ <- ZIO.when(maybeSupportedEvent.isEmpty)(inputRecord.offset.commit).mapError(AppError.fromThrowable)
          supportedEvent <- ZIO
            .fromOption(maybeSupportedEvent)
            .mapError(_ => AppError.fromMessage(s"Universe $universe of message : ${inputEvent.header.id} is not supported"))
          outputEvent <- EventProcessor.handleInput(
            inputRecord.record.topic,
            supportedEvent,
          )
        } yield outputEvent

        result.foldM(appError => Log.warnIO(appError.logMessage) *> ZIO.succeed[Option[OutputEvent]](None), oe => ZIO.succeed(Option(oe)))
      }
      .mapError(AppError.fromThrowable)

  val publishEvent: ZTransducer[Has[EventPublisher] & Has[AppConfig] & Has[
    ZioSerde[Any, OutputEvent],
  ], AppError, OutputCommittable, OutputCommittable] = {
    implicit val facetKey: HasKey[OutputEvent] = (_: OutputEvent).body.newValues.id
    Committable
      .mapValueM((event: OutputEvent) =>
        for {
          publisher <- ZIO.service[EventPublisher]
          cfg       <- ZIO.service[AppConfig].map(_.processorSettings)
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

  val pipeline: ZSink[Env.Processor, AppError, StringRecord, OutputCommittable, Unit] =
    inputToOutput >>> publishEvent >>> commit

  val run: ZIO[Env.Processor, AppError, Unit] =
    inputRecordEvents >>> pipeline
}
