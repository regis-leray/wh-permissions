package com.williamhill.permission.kafka.events.generic

import com.github.andyglow.jsonschema.AsCirce.*
import com.williamhill.permission.domain.{Action, FacetContext, PermissionDenial}
import io.circe.*
import io.circe.generic.semiauto.deriveCodec

import json.schema.Version.*
import json.{Schema, Json as JsonSchema}

import com.whbettingengine.kafka.serialization.json.schema.HasSchema

final case class OutputEvent(header: Header, body: OutputBody)

object OutputEvent {

  implicit val codec: Codec[OutputEvent] = deriveCodec

  val schema: Schema[OutputEvent] = {
    @scala.annotation.nowarn("msg=never used")
    implicit val metaSchema =
      json.Schema.`object`.Free[JsonObject]().toDefinition("json-object")

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
      data = Data(permissionDenials = facetContext.denials, actions = facetContext.actions),
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
    permissionDenials: List[PermissionDenial],
    actions: List[Action],
)
object Data {

  implicit val codec: Codec[Data] = deriveCodec

}
case class Details(
    reason: String,
)
object Details {

  implicit val codec: Codec[OutputEvent] = deriveCodec

}

/** {
  *    "type": "SelfExclusion",
  *    "newValues": {
  *      "id": "U00000001",
  *      "universe": "wh-eu-de",
  *      "data": {
  *         "permissionDenials": {
  *          "canWithdraw": [
  *            {
  *              "reasonCode": "account-not-verified",
  *              "description": "The account is not verified",
  *              "details": {
  *                "reason": "unverified"
  *              }
  *            }
  *          ]
  *        },
  *        "actions": [
  *          {
  *            "name": "sendVerificationFulfillmentReminder",
  *            "reason": "sendVerificationFulfillmentReminder",
  *            "relatesToPermissions": [
  *              "canWithdraw"
  *            ],
  *            "type": "notification",
  *            "deadline": "2022-12-12T20:19:26.117829Z"
  *          }
  *        ]
  *      }
  *    }
  * }
  */
