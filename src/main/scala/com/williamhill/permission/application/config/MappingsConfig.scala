package com.williamhill.permission.application.config

import java.time.Instant

import com.williamhill.permission.domain.PlayerId
import com.williamhill.permission.dsl.{BooleanExpression, Expression, Value}
import pureconfig.generic.semiauto.deriveReader
import pureconfig.{ConfigReader, ConfigSource}
import zio.blocking.{Blocking, blocking}
import zio.{Has, URLayer, ZIO}

case class EventTypeMapping(value: String, when: BooleanExpression) {
  def toExpression: Expression.Basic[String] = Expression.Basic(Value.Const(value), when = Some(when))
}

object EventTypeMapping {
  implicit val reader: ConfigReader[EventTypeMapping] = deriveReader
}

case class Mapping(
    topics: Option[Set[String]],
    eventType: EventTypeMapping,
    status: Expression[String],
    playerId: Expression.Basic[PlayerId],
    actionsStart: Option[Expression.Basic[Instant]],
    actionsEnd: Option[Expression.Basic[Instant]],
)

case class MappingsConfig(mappings: List[Mapping])

object MappingsConfig {
  implicit val mappingReader: ConfigReader[Mapping] = deriveReader
  implicit val reader: ConfigReader[MappingsConfig] = deriveReader

  val layer: URLayer[Blocking, Has[MappingsConfig]] =
    blocking(ZIO.effect(ConfigSource.resources("mappings.conf").loadOrThrow[MappingsConfig])).orDie.toLayer
}
