package com.williamhill.permission.dsl

import io.circe.Json
import pureconfig.ConfigReader

object JsonConfigReader {

  implicit val jsonReader: ConfigReader[Json] = ConfigReader.fromCursor { cursor =>
    cursor.asInt
      .map(Json.fromInt)
      .orElse(cursor.asLong.map(Json.fromLong))
      .orElse(cursor.asDouble.map(Json.fromDoubleOrString))
      .orElse(cursor.asBoolean.map(Json.fromBoolean))
      .orElse(cursor.asString.map(Json.fromString))
  }

}
