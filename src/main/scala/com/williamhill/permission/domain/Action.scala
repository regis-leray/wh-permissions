package com.williamhill.permission.domain

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

import java.time.Instant

final case class Action(
    `type`: String,
    name: String,
    reasonCode: String,
    denialDescription: String,
    deniedPermissions: List[String],
    deadline: Option[Instant] = None,
)

object Action {
  implicit val codec: Codec[Action] = deriveCodec[Action]
}
