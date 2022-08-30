package com.williamhill.permission.kafka

import com.whbettingengine.kafka.serialization.json.schema.HasSchema
import io.circe.Encoder
import org.apache.kafka.clients.producer.ProducerRecord
import zio.*
import zio.kafka.producer.Producer
import zio.kafka.serde.Serde as ZioSerde

trait EventPublisher {
  def publish[T: Encoder: HasSchema: HasKey: Tag](topic: String, value: T): RIO[Has[ZioSerde[Any, T]], Unit]
}

class EventPublisherLive(producer: Producer) extends EventPublisher {

  override def publish[T: Encoder: HasSchema: HasKey: Tag](topic: String, value: T): RIO[Has[ZioSerde[Any, T]], Unit] = {
    import HasKey.*
    val producerRecord = new ProducerRecord(topic, value.stringKey, value)
    for {

      serde          <- ZIO.service[ZioSerde[Any, T]]
      recordMetadata <- producer.produce(producerRecord, ZioSerde.string, serde)
      _ = println(s"*** EventPublisher publish: published recordMetadata: $recordMetadata")
    } yield ()
  }
}

object EventPublisher {
  val layer: URLayer[Has[Producer], Has[EventPublisher]] =
    (new EventPublisherLive(_)).toLayer
}
