package com.williamhill.permission.kafka.events.generic

import com.github.andyglow.jsonschema.AsCirce.*
import com.whbettingengine.kafka.serialization.json.schema.HasSchema
import com.williamhill.permission.domain.{FacetContext, OutputAction, PermissionDenial}
import io.circe.*
import io.circe.generic.semiauto.deriveCodec
import json.schema.Version.*
import json.{Json => JsonSchema, Schema}

final case class OutputEvent(header: Header, body: OutputBody)

object OutputEvent {

  implicit val codec: Codec[OutputEvent] = deriveCodec

  val schema: Schema[OutputEvent] = {
    @scala.annotation.nowarn("msg=never used")
    implicit val metaSchema: Schema[JsonObject] = json.Schema.`object`.Free[JsonObject]().toDefinition("json-object")
    JsonSchema.schema[OutputEvent]
  }

  implicit val hasSchema: HasSchema[OutputEvent] = (_: OutputEvent) =>
    new io.confluent.kafka.schemaregistry.json.JsonSchema(
      schema.asCirce(Draft04()).spaces2,
    )

  def apply(facetContext: FacetContext): OutputEvent =
    OutputEvent(facetContext.header, OutputBody(facetContext))

}

case class OutputBody(
    `type`: String,
    newValues: NewValues,
)

object OutputBody {
  implicit val codec: Codec[OutputBody] = deriveCodec

  def apply(
      facetContext: FacetContext,
  ): OutputBody = OutputBody(
    `type` = facetContext.name,
    newValues = NewValues(
      id = facetContext.playerId.value,
      universe = facetContext.universe.value.toLowerCase,
      data = Data(
        permissionDenials = facetContext.denials,
        actions = facetContext.outputActions,
        context = Map.empty,
      ),
    ),
  )
}

case class NewValues(
    id: String,
    universe: String,
    data: Data,
)

object NewValues {
  implicit val codec: Codec[NewValues] = deriveCodec
}

case class Data(
    permissionDenials: Map[String, Vector[PermissionDenial]],
    actions: Vector[OutputAction],
    context: Map[String, String],
)

object Data {
  implicit val codec: Codec[Data] = deriveCodec
}
