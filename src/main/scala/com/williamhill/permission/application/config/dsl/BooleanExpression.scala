package com.williamhill.permission.application.config.dsl

import io.circe.Json
import pureconfig.ConfigReader
import pureconfig.error.{ConfigReaderFailures, ConvertFailure, KeyNotFound}

sealed trait BooleanExpression

object BooleanExpression extends JsonReader {
  case class Equals(equals: List[MappingValue[Json]]) extends BooleanExpression
  case class Defined(defined: MappingValue.Path)      extends BooleanExpression
  case class Or(or: List[BooleanExpression])          extends BooleanExpression
  case class And(and: List[BooleanExpression])        extends BooleanExpression

  private def mkReader(key: String, reader: ConfigReader[BooleanExpression]): ConfigReader[BooleanExpression] =
    ConfigReader.fromCursor(cursor =>
      for {
        obj        <- cursor.asMap
        dataCursor <- obj.get(key).toRight(ConfigReaderFailures(ConvertFailure(KeyNotFound(key, obj.keys.toSet), cursor)))
        expression <- reader.from(dataCursor)
      } yield expression,
    )

  val equalsReader: ConfigReader[BooleanExpression]  = mkReader("equals", ConfigReader[List[MappingValue[Json]]].map(Equals))
  val definedReader: ConfigReader[BooleanExpression] = mkReader("defined", MappingValue.Path.reader.map(Defined))
  def andReader: ConfigReader[BooleanExpression]     = mkReader("and", ConfigReader[List[BooleanExpression]].map(And))
  def orReader: ConfigReader[BooleanExpression]      = mkReader("or", ConfigReader[List[BooleanExpression]].map(Or))

  implicit def reader: ConfigReader[BooleanExpression] =
    equalsReader.orElse(definedReader).orElse(andReader).orElse(orReader)
}
