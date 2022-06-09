package com.williamhill.permissions_ep

import zio.*
import zio.kafka.producer.Producer
import zio.kafka.serde.Serde as ZioSerde

import com.williamhill.permission.Processor
import com.williamhill.permission.kafka.events.generic.InputEvent
import org.apache.kafka.clients.producer.ProducerRecord

trait TestInputEventProducer {
  def publishEvent(event: InputEvent): RIO[Has[ZioSerde[Any, InputEvent]], Unit]
}

object TestInputEventProducer {
  val layer: URLayer[Has[Processor.Config] & Has[Producer], Has[TestInputEventProducer]] =
    ZLayer.fromServices[Processor.Config, Producer, TestInputEventProducer](TestInputEventProducerLive)
}

final case class TestInputEventProducerLive(
    config: Processor.Config,
    kafkaProducer: Producer,
) extends TestInputEventProducer {
  override def publishEvent(event: InputEvent): RIO[Has[ZioSerde[Any, InputEvent]], Unit] = for {
    zioSerde <- ZIO.service[ZioSerde[Any, InputEvent]]
    record = new ProducerRecord(config.inputEvents.topics.head, event.header.id, event)
    _ <- kafkaProducer.produce(record, ZioSerde.string, zioSerde)
  } yield ()
}
