package com.williamhill.permission.domain

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

case class PermissionDenial(
    reason: String,
    description: String,
    permissionName: String,
)

object PermissionDenial {

  implicit val codec: Codec[PermissionDenial] = deriveCodec

}
