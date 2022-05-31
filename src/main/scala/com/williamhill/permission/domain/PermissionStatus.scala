package com.williamhill.permission.domain

import java.time.Instant

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

final case class PermissionStatus(
    statuses: Vector[String],
    startDate: Option[Instant] = None,
    endDate: Option[Instant] = None,
)

object PermissionStatus {
  implicit val codec: Codec[PermissionStatus] = deriveCodec
}
