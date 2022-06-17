package com.williamhill.permission.application.config

import com.williamhill.permission.dsl.Expression
import pureconfig.generic.semiauto.deriveReader
import pureconfig.{ConfigReader, ConfigSource}
import zio.blocking.{Blocking, blocking}
import zio.{Has, RLayer, ZIO}

final case class ActionDefinition(
    name: String,
    `type`: String,
    reasonCode: String,
    denialDescription: String,
    deniedPermissions: List[String],
)

object ActionDefinition {
  implicit val reader: ConfigReader[ActionDefinition] = deriveReader
}

final case class RulesConfig(
    rules: Vector[Expression],
    actions: Vector[ActionDefinition],
)

object RulesConfig {
  implicit val reader: ConfigReader[RulesConfig] = deriveReader

  val live: RLayer[Blocking, Has[RulesConfig]] =
    blocking(ZIO.effect(ConfigSource.resources("rules.conf").loadOrThrow[RulesConfig])).toLayer
}
