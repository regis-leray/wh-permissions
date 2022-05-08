package com.williamhill.permission.domain

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

import java.time.Instant

final case class Action(
    name: String,
    reason: String,
    deniedPermissions: List[String],
    deadline: Option[Instant] = None,
    denialDescription: Option[String] = None,
) {
  def withDeadline(deadline: Instant): Action = copy(deadline = Some(deadline))
}

object Action {
  implicit val codec: Codec[Action] = deriveCodec[Action]
}
