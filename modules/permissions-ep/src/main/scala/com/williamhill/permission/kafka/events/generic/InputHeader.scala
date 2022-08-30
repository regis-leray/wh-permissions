package com.williamhill.permission.kafka.events.generic

import java.time.Instant

import io.circe.*
import io.circe.generic.semiauto.*

final case class InputHeader(
    id: String,
    who: Who,
    universe: String,
    when: Instant,
    sessionId: Option[String],
    traceId: Option[String],
)

object InputHeader {
  implicit val codec: Codec[InputHeader] = {
    val codec = deriveCodec[InputHeader]
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
