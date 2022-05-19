package com.williamhill.permission.application.config

import pureconfig.error.CannotConvert
import pureconfig.generic.semiauto.deriveReader
import pureconfig.{ConfigReader, ConfigSource}
import zio.blocking.{Blocking, blocking}
import zio.{Has, URLayer, ZIO}

sealed trait MappingValue

object MappingValue {
  case class Path(path: String)       extends MappingValue
  case class Hardcoded(value: String) extends MappingValue

  object Path {
    implicit val reader: ConfigReader[Path] = ConfigReader.stringConfigReader.emap {
      case s if s.startsWith("$.") => Right(Path(s.drop(2)))
      case s                       => Left(CannotConvert(s, "Path", s"Invalid json path format: $s"))
    }
  }

  object Hardcoded {
    implicit val reader: ConfigReader[Hardcoded] = ConfigReader.stringConfigReader.map {
      case s if s.startsWith("\\$.") => Hardcoded(s.drop(1))
      case s                         => Hardcoded(s)
    }
  }

  implicit val reader: ConfigReader[MappingValue] = Path.reader.orElse(Hardcoded.reader)
}

sealed trait MappingExpression

object MappingExpression {

  sealed trait SingleExpression extends MappingExpression {
    def value: MappingValue
  }

  case class SimpleExpression(value: MappingValue) extends SingleExpression

  object SimpleExpression {
    implicit val reader: ConfigReader[SimpleExpression] =
      MappingValue.reader.map(SimpleExpression(_))
  }

  sealed trait ConditionalExpression extends SingleExpression

  object ConditionalExpression {
    case class WhenEquals(value: MappingValue, whenEquals: List[MappingValue])  extends ConditionalExpression
    case class WhenDefined(value: MappingValue, whenDefined: MappingValue.Path) extends ConditionalExpression

    implicit val reader: ConfigReader[ConditionalExpression] =
      deriveReader[WhenEquals]
        .orElse(deriveReader[WhenDefined])
  }

  object SingleExpression {
    implicit val reader: ConfigReader[SingleExpression] =
      ConditionalExpression.reader.orElse(SimpleExpression.reader)
  }

  case class ExpressionList(expressions: List[SingleExpression]) extends MappingExpression

  object ExpressionList {
    implicit val reader: ConfigReader[ExpressionList] =
      ConfigReader[List[SingleExpression]].map(ExpressionList(_))
  }

  implicit val reader: ConfigReader[MappingExpression] = ExpressionList.reader.orElse(SingleExpression.reader)
}

case class Mapping(
    eventType: String,
    status: MappingExpression,
    playerId: MappingValue.Path,
    actionsStart: Option[List[MappingValue.Path]],
    actionsEnd: Option[List[MappingValue.Path]],
)

case class MappingsConfig(mappings: List[Mapping])

object MappingsConfig {
  implicit val mappingReader: ConfigReader[Mapping] = deriveReader
  implicit val reader: ConfigReader[MappingsConfig] = deriveReader

  val layer: URLayer[Blocking, Has[MappingsConfig]] =
    blocking(ZIO.effect(ConfigSource.resources("mappings.conf").loadOrThrow[MappingsConfig])).orDie.toLayer
}
