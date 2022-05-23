package com.williamhill.permission.application.config

import java.time.Instant

import com.williamhill.permission.application.config.dsl.MappingExpression
import com.williamhill.permission.domain.PlayerId
import pureconfig.generic.semiauto.deriveReader
import pureconfig.{ConfigReader, ConfigSource}
import zio.blocking.{Blocking, blocking}
import zio.{Has, URLayer, ZIO}

case class Mapping(
    eventType: String,
    status: MappingExpression[String],
    playerId: MappingExpression.Single[PlayerId],
    actionsStart: Option[MappingExpression.Single[Instant]],
    actionsEnd: Option[MappingExpression.Single[Instant]],
)

case class MappingsConfig(mappings: List[Mapping])

object MappingsConfig {
  implicit val mappingReader: ConfigReader[Mapping] = deriveReader
  implicit val reader: ConfigReader[MappingsConfig] = deriveReader

  val layer: URLayer[Blocking, Has[MappingsConfig]] =
    blocking(ZIO.effect(ConfigSource.resources("mappings.conf").loadOrThrow[MappingsConfig])).orDie.toLayer
}
