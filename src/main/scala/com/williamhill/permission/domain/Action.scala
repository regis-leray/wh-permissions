package com.williamhill.permission.domain

import java.time.Instant

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

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
