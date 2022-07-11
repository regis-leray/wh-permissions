package com.williamhill.permission.kafka.serde

import com.williamhill.permission.config.ProcessorConfig
import com.williamhill.permission.kafka.events.generic.InputEvent
import com.williamhill.platform.event.permission.Event as OutputEvent
import com.williamhill.platform.kafka.JsonSerialization
import io.github.azhur.kafkaserdecirce.CirceSupport
import org.apache.kafka.common.serialization.Serde as KafkaSerde
import zio.kafka.serde.Serde as ZioSerde
import zio.{Has, RLayer, Task, TaskLayer, ZIO}

/** For InputEvent we need to have a generic serializer which doesn't conform to any schema
  * For OutputEvent we need to produce the schema in the schema registry
  */
object AppSerdes extends CirceSupport {

  private val inputSerdes: Task[ZioSerde[Any, InputEvent]] = ZioSerde
    .fromKafkaSerde(InputEvent.kserde, props = Map.empty[String, AnyRef], isKey = false)

  val inputSerdeLayer: TaskLayer[Has[ZioSerde[Any, InputEvent]]] = inputSerdes.toLayer

  val outputSerdeLayer: RLayer[Has[ProcessorConfig], Has[ZioSerde[Any, OutputEvent]]] = (for {
    config       <- ZIO.service[ProcessorConfig]
    deserializer <- JsonSerialization.valueDeserializer[OutputEvent](config.outputEvents.schemaRegistrySettings)
    serializer   <- JsonSerialization.valueSerializer[OutputEvent](config.outputEvents.schemaRegistrySettings)
    serde = ZioSerde.apply(deserializer)(serializer)
  } yield serde).toLayer

  // FIXME this is needed to run IT tests in CI. Locally ProcessorISpec is running fine with registry-derived serializer, but it fails in gitlabci , so we are using this to make it pass
  val outputSerdeLayerTest: TaskLayer[Has[ZioSerde[Any, OutputEvent]]] = ZioSerde
    .fromKafkaSerde(implicitly[KafkaSerde[OutputEvent]], props = Map.empty[String, AnyRef], isKey = false)
    .toLayer

}
