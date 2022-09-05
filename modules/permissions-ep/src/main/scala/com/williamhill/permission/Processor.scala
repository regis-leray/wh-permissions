package com.williamhill.permission

import cats.implicits.catsSyntaxEitherId
import com.github.mlangc.slf4zio.api.{Logging, logging as Log}
import com.williamhill.permission.application.{AppError, Env}
import com.williamhill.permission.config.ProcessorConfig
import com.williamhill.permission.db.postgres.Postgres
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
import zio.blocking.Blocking
import zio.clock.Clock

object Processor {

  val inputRecordEvents: ZStream[Has[Consumer] & Has[ProcessorConfig], AppError, StringRecord] = {

    (for {
      config <- ZStream.service[ProcessorConfig]
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

  val inputToOutput
      : ZTransducer[Has[EventRuleProcessor] & Clock & Blocking & Postgres & Logging, AppError, StringRecord, OutputCommittable] =
    Committable
      .filterMapCommittableRecordM { (inputRecord: StringRecord) =>
        (for {
          processor  <- ZIO.service[EventRuleProcessor]
          inputEvent <- ZIO.fromEither(InputEvent.produce(inputRecord.record.value()))

          outputEvent <- processor
            .handle(inputEvent)
            .mapError(AppError.fromThrowable)
        } yield outputEvent).tapError(err => Log.warnIO(err.logMessage))
      }
      .mapError(AppError.fromThrowable)

  val publishEvent: ZTransducer[Has[EventPublisher] & Has[ProcessorConfig] & Has[
    ZioSerde[Any, OutputEvent],
  ], AppError, OutputCommittable, OutputCommittable] = {
    implicit val facetKey: HasKey[OutputEvent] = (_: OutputEvent).body.newValues.id
    Committable
      .mapValueM((event: OutputEvent) =>
        for {
          publisher <- ZIO.service[EventPublisher]
          cfg       <- ZIO.service[ProcessorConfig]
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

  val run: ZIO[Env.Core & Has[ProcessorConfig] & Clock & Blocking, AppError, Unit] =
    inputRecordEvents.run(inputToOutput >>> publishEvent >>> commit)

}
