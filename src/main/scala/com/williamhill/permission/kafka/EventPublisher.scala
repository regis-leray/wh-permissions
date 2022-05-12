package com.williamhill.permission.kafka

import zio.*
import zio.kafka.producer.Producer
import zio.kafka.serde.Serde

import com.williamhill.permission.Processor
import com.williamhill.platform.kafka
import io.circe.Encoder
import org.apache.kafka.clients.producer.ProducerRecord

import com.whbettingengine.kafka.serialization.json.schema.HasSchema

trait EventPublisher {
  def publish[T: Encoder: HasSchema: HasKey](topic: String, value: T): Task[Unit]
}

class EventPublisherLive(producer: Producer, config: Processor.Config) extends EventPublisher {

  override def publish[T: Encoder: HasSchema: HasKey](topic: String, value: T): Task[Unit] = {
    import HasKey.*
    for {
      valueSerializer <- kafka.JsonSerialization.valueSerializer[T](
        config.outputEvents.schemaRegistrySettings,
      )
      producerRecord = new ProducerRecord(topic, value.stringKey, value)
      _ <- producer.produce(producerRecord, Serde.string, valueSerializer)
    } yield ()
  }
}

object EventPublisherLive {
  val layer: URLayer[Has[Producer] & Has[Processor.Config], Has[EventPublisher]] =
    (new EventPublisherLive(_, _)).toLayer
}
