package com.williamhill.permission.kafka.events.generic

import java.time.Instant

import io.circe.*
import io.circe.generic.semiauto.*

final case class Header(
    id: String,
    who: Who,
    universe: String,
    when: Instant,
    sessionId: Option[String],
    traceId: Option[String],
)

object Header {
  implicit val codec: Codec[Header] = {
    val codec = deriveCodec[Header]
    Codec.from(codec, codec.mapJson(_.dropNullValues))
  }
}

final case class Who(id: String, name: String, `type`: String, ip: Option[String])

object Who {
  implicit val codec: Codec[Who] = {
    val codec = deriveCodec[Who]
    Codec.from(codec, codec.mapJson(_.dropNullValues))
  }
}
