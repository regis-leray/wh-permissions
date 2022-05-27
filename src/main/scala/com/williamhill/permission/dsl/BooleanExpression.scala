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

  case class Equals(values: List[Value[Json]]) extends BooleanExpression
  case class Defined(path: Value.JsonPath)     extends BooleanExpression
  case class Or(any: List[BooleanExpression])  extends BooleanExpression
  case class And(all: List[BooleanExpression]) extends BooleanExpression

  private def mkReader(key: String, reader: ConfigReader[BooleanExpression]): ConfigReader[BooleanExpression] =
    ConfigReader.fromCursor(cursor =>
      for {
        obj        <- cursor.asMap
        dataCursor <- obj.get(key).toRight(ConfigReaderFailures(ConvertFailure(KeyNotFound(key, obj.keys.toSet), cursor)))
        expression <- reader.from(dataCursor)
      } yield expression,
    )

  val equalsReader: ConfigReader[BooleanExpression]  = mkReader("equals", ConfigReader[List[Value[Json]]].map(Equals))
  val definedReader: ConfigReader[BooleanExpression] = mkReader("defined", Value.JsonPath.reader.map(Defined))
  def andReader: ConfigReader[BooleanExpression]     = mkReader("all", ConfigReader[List[BooleanExpression]].map(And))
  def orReader: ConfigReader[BooleanExpression]      = mkReader("any", ConfigReader[List[BooleanExpression]].map(Or))

  implicit def reader: ConfigReader[BooleanExpression] =
    equalsReader.orElse(definedReader).orElse(andReader).orElse(orReader)
}
