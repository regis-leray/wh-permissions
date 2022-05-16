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

final case class ActionBinding(
    universe: String,
    eventType: String,
    status: String,
    actions: List[String],
)

object ActionBinding {
  implicit val reader: ConfigReader[ActionBinding] = deriveReader
}

final case class ActionsConfig(
    bindings: List[ActionBinding],
    definitions: List[ActionDefinition],
)

object ActionsConfig {
  implicit val reader: ConfigReader[ActionsConfig] = deriveReader

  val layer: URLayer[Blocking, Has[ActionsConfig]] =
    blocking(ZIO.effect(ConfigSource.resources("actions.conf").loadOrThrow[ActionsConfig])).orDie.toLayer
}
