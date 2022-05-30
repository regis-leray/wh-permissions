package com.williamhill.permission.domain

import io.circe.Decoder
import pureconfig.ConfigReader

final case class PlayerId(value: String) {
  override def toString: String = value
}

object PlayerId {
  implicit val decoder: Decoder[PlayerId]     = Decoder.decodeString.map(PlayerId(_))
  implicit val reader: ConfigReader[PlayerId] = ConfigReader.stringConfigReader.map(PlayerId(_))
}
