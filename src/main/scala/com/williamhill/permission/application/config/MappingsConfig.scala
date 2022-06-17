package com.williamhill.permission.application.config

import com.williamhill.permission.dsl.Expression.Const
import com.williamhill.permission.dsl.{BooleanExpression, Expression}
import pureconfig.generic.semiauto.deriveReader
import pureconfig.{ConfigReader, ConfigSource}
import zio.blocking.{Blocking, blocking}
import zio.{Has, RLayer, ZIO}

case class EventTypeMapping(value: String, when: BooleanExpression) {
  def toExpression: Expression = Expression.Conditional(Const(value), when = Some(when))
}

object EventTypeMapping {
  implicit val reader: ConfigReader[EventTypeMapping] = deriveReader
}

case class Mapping(
    topics: Option[Set[String]],
    event: EventTypeMapping,
    status: Expression,
    playerId: Expression,
    actionsStart: Option[Expression],
    actionsEnd: Option[Expression],
)

case class MappingsConfig(mappings: List[Mapping])

object MappingsConfig {
  implicit val mappingReader: ConfigReader[Mapping] = deriveReader
  implicit val reader: ConfigReader[MappingsConfig] = deriveReader

  val live: RLayer[Blocking, Has[MappingsConfig]] =
    blocking(ZIO.effect(ConfigSource.resources("mappings.conf").loadOrThrow[MappingsConfig])).toLayer
}
