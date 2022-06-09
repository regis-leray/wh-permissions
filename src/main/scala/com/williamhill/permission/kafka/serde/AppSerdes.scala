package com.williamhill.permission.kafka.serde

import com.williamhill.permission.Processor.Config
import com.williamhill.permission.kafka.events.generic.{InputEvent, OutputEvent}
import com.williamhill.platform.kafka.JsonSerialization
import io.github.azhur.kafkaserdecirce.CirceSupport
import org.apache.kafka.common.serialization.Serde as KafkaSerde
import zio.kafka.serde.Serde as ZioSerde
import zio.{Has, RLayer, TaskLayer, ZIO}

/** For InputEvent we need to have a generic serializer which doesn't conform to any schema
  * For OutputEvent we need to produce the schema in the schema registry
  */
object AppSerdes extends CirceSupport {

  val inputSerdeLayer: TaskLayer[Has[ZioSerde[Any, InputEvent]]] = ZioSerde
    .fromKafkaSerde(implicitly[KafkaSerde[InputEvent]], props = Map.empty[String, AnyRef], isKey = false)
    .toLayer

  val outputSerdeLayer: RLayer[Has[Config], Has[ZioSerde[Any, OutputEvent]]] = (for {
    config       <- ZIO.service[Config]
    deserializer <- JsonSerialization.valueDeserializer[OutputEvent](config.outputEvents.schemaRegistrySettings)
    serializer   <- JsonSerialization.valueSerializer[OutputEvent](config.outputEvents.schemaRegistrySettings)
    serde = ZioSerde.apply(deserializer)(serializer)
  } yield serde).toLayer

  // FIXME this is needed to run IT tests in CI. Locally ProcessorISpec is running fine with registry-derived serializer, but it fails in gitlabci , so we are using this to make it pass
  val outputSerdeLayerTest: TaskLayer[Has[ZioSerde[Any, OutputEvent]]] = ZioSerde
    .fromKafkaSerde(implicitly[KafkaSerde[OutputEvent]], props = Map.empty[String, AnyRef], isKey = false)
    .toLayer

}
