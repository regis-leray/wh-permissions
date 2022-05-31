package com.williamhill.permission.dsl

import io.circe.Json
import pureconfig.ConfigReader
import pureconfig.error.{ConfigReaderFailures, ConvertFailure, KeyNotFound}

sealed trait BooleanExpression

object BooleanExpression {
  implicit private val jsonReader: ConfigReader[Json] = ConfigReader.fromCursor { cursor =>
    cursor.asInt
      .map(Json.fromInt)
      .orElse(cursor.asLong.map(Json.fromLong))
      .orElse(cursor.asDouble.map(Json.fromDoubleOrString))
      .orElse(cursor.asBoolean.map(Json.fromBoolean))
      .orElse(cursor.asString.map(Json.fromString))
  }

  case class Equals(src: Expression[Json], other: Expression[Json])   extends BooleanExpression
  case class Includes(src: Expression[Json], other: Expression[Json]) extends BooleanExpression
  case class OneOf(src: Expression[Json], other: Expression[Json])    extends BooleanExpression
  case class Defined(path: Expression.JsonPath)                       extends BooleanExpression
  case class And(all: List[BooleanExpression])                        extends BooleanExpression
  case class Or(any: List[BooleanExpression])                         extends BooleanExpression

  private def propertyReader[A](key: String, reader: ConfigReader[A]): ConfigReader[A] = {
    ConfigReader.fromCursor { cursor =>
      for {
        obj         <- cursor.asMap
        valueCursor <- obj.get(key).toRight(ConfigReaderFailures(ConvertFailure(KeyNotFound(key, obj.keys.toSet), cursor)))
        decoded     <- reader.from(valueCursor)
      } yield decoded
    }
  }

  val equalsReader: ConfigReader[Equals] =
    propertyReader("src", Expression.reader[Json])
      .zip(propertyReader("equals", Expression.reader[Json]))
      .map { case (src, equalsTo) => Equals(src, equalsTo) }

  val includesReader: ConfigReader[Includes] =
    propertyReader("src", Expression.reader[Json])
      .zip(propertyReader("includes", Expression.reader[Json]))
      .map { case (src, equalsTo) => Includes(src, equalsTo) }

  val oneOfReader: ConfigReader[OneOf] =
    propertyReader("src", Expression.reader[Json])
      .zip(propertyReader("one-of", Expression.reader[Json]))
      .map { case (src, equalsTo) => OneOf(src, equalsTo) }

  val definedReader: ConfigReader[BooleanExpression] = propertyReader("defined", Expression.JsonPath.reader.map(Defined))

  def andReader: ConfigReader[BooleanExpression] = propertyReader("all", ConfigReader[List[BooleanExpression]].map(And))
  def orReader: ConfigReader[BooleanExpression]  = propertyReader("any", ConfigReader[List[BooleanExpression]].map(Or))

  implicit def reader: ConfigReader[BooleanExpression] =
    equalsReader
      .orElse(definedReader)
      .orElse(includesReader)
      .orElse(oneOfReader)
      .orElse(andReader)
      .orElse(orReader)
}
