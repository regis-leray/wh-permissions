package com.williamhill.permission.kafka.events.generic

import com.github.andyglow.jsonschema.AsCirce.*
import com.williamhill.permission.domain.{Action, FacetContext, PermissionDenial}
import io.circe.*
import io.circe.generic.semiauto.deriveCodec

import json.schema.Version.*
import json.{Schema, Json as JsonSchema}

import com.whbettingengine.kafka.serialization.json.schema.HasSchema
import com.williamhill.permission.domain.PermissionStatus

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
      id = facetContext.header.id,
      universe = facetContext.universe.toString.toLowerCase,
      data = Data(status = facetContext.newStatus, permissionDenials = facetContext.denials, actions = facetContext.actions),
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
    status: PermissionStatus,
    permissionDenials: List[PermissionDenial],
    actions: List[Action],
)

object Data {
  implicit val codec: Codec[Data] = deriveCodec
}
