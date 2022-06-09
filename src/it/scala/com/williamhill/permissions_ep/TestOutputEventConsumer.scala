package com.williamhill.permissions_ep

import zio.clock.Clock
import zio.kafka.consumer.{Consumer, Subscription}
import zio.kafka.serde.Serde as ZioSerde
import zio.stream.ZStream
import zio.{Has, ZIO, ZLayer}

import com.williamhill.permission.Processor
import com.williamhill.permission.application.AppError
import com.williamhill.permission.kafka.events.generic.OutputEvent
trait TestOutputEventConsumer {
  def eventCount: ZIO[Has[Consumer] & Has[ZioSerde[Any, OutputEvent]] & Clock, AppError, Unit]
}

object TestOutputEventConsumer {
  var size: Long = 0
  val layer: ZLayer[Has[Processor.Config] & Has[Consumer], Nothing, Has[TestOutputEventConsumer]] =
    ZLayer.fromServices[Processor.Config, Consumer, TestOutputEventConsumer](TestOutputEventConsumerLive)
}

final case class TestOutputEventConsumerLive(
    config: Processor.Config,
    consumer: Consumer,
) extends TestOutputEventConsumer {
  override def eventCount: ZIO[Has[Consumer] & Has[ZioSerde[Any, OutputEvent]] & Clock, AppError, Unit] =
    collectOutputEventStream

  private def collectOutputEventStream: ZIO[Has[Consumer] & Has[ZioSerde[Any, OutputEvent]] & Clock, AppError, Unit] = {

    (for {

      zioSerde <- ZStream.service[ZioSerde[Any, OutputEvent]]
      size <- Consumer
        .subscribeAnd(Subscription.topics(config.outputEvents.topics.head))
        .plainStream(ZioSerde.string, zioSerde)
        .tap { rec =>
          TestOutputEventConsumer.size = TestOutputEventConsumer.size + 1
          rec.offset.commit
        }
    } yield size)
      .mapError(AppError.fromThrowable)
      .runDrain

  }
}
