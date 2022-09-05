package com.williamhill.permission.config

import pureconfig.ConfigReader
import pureconfig.generic.semiauto.deriveReader

final case class DbConfig(url: String, username: String, password: String)

object DbConfig {
  implicit val reader: ConfigReader[DbConfig] = deriveReader
}
