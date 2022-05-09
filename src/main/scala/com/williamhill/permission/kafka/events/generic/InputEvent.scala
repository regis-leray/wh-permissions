package com.williamhill.permission.kafka.events.generic

import cats.Eq

import io.circe.*
import io.circe.generic.semiauto.deriveCodec

final case class InputEvent(header: Header, body: Json)

object InputEvent {
  implicit val codec: Codec[InputEvent] = deriveCodec
  implicit val eq: Eq[InputEvent]       = Eq.fromUniversalEquals[InputEvent]
}
