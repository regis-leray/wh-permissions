package com.williamhill.permission.application.config

import pureconfig.generic.semiauto.deriveReader
import pureconfig.{ConfigReader, ConfigSource}
import zio.blocking.{Blocking, blocking}
import zio.{Has, URLayer, ZIO}

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

final case class RuleDefinition(
    universe: String,
    eventType: String,
    status: String,
    actions: List[String],
)

object RuleDefinition {
  implicit val reader: ConfigReader[RuleDefinition] = deriveReader
}

final case class RulesConfig(
    rules: List[RuleDefinition],
    actions: List[ActionDefinition],
)

object RulesConfig {
  implicit val reader: ConfigReader[RulesConfig] = deriveReader

  val layer: URLayer[Blocking, Has[RulesConfig]] =
    blocking(ZIO.effect(ConfigSource.resources("rules.conf").loadOrThrow[RulesConfig])).orDie.toLayer
}
