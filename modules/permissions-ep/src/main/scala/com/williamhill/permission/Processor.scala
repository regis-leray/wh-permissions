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
import io.circe.syntax.*

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

  val inputToOutput: ZTransducer[Has[EventRuleProcessor] & Logging, AppError, StringRecord, OutputCommittable] =
    Committable
      .filterMapCommittableRecordM { (inputRecord: StringRecord) =>
        (for {
          processor  <- ZIO.service[EventRuleProcessor]
          inputEvent <- ZIO.fromEither(InputEvent.produce(inputRecord.record.value()))

          outputEvent <- processor
            .handle(inputEvent)
            .flatMap {
              case output :: Nil => ZIO.some(output._2)
              case Nil           => ZIO.none
              case others =>
                ZIO.fail(
                  new Exception(
                    s"Error in permissions rules definition ! Duplicate rules ${others.map(_._1.name)} applies for same event : ${inputEvent.asJson.noSpaces}",
                  ),
                )
            }
            .mapError(AppError.fromThrowable)
        } yield outputEvent).tapError(err => Log.warnIO(err.logMessage))
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
