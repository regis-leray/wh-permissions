package com.williamhill.permission.dsl

import pureconfig.ConfigReader
import pureconfig.error.{ConfigReaderFailures, ConvertFailure, KeyNotFound}

sealed trait BooleanExpression

object BooleanExpression {

  case class Equals(src: Expression, other: Expression)   extends BooleanExpression
  case class Includes(src: Expression, other: Expression) extends BooleanExpression
  case class OneOf(src: Expression, other: Expression)    extends BooleanExpression
  case class Defined(path: Expression.JsonPath)           extends BooleanExpression
  case class And(all: List[BooleanExpression])            extends BooleanExpression
  case class Or(any: List[BooleanExpression])             extends BooleanExpression

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
    propertyReader("src", Expression.reader)
      .zip(propertyReader("equals", Expression.reader))
      .map { case (src, equalsTo) => Equals(src, equalsTo) }

  val includesReader: ConfigReader[Includes] =
    propertyReader("src", Expression.reader)
      .zip(propertyReader("includes", Expression.reader))
      .map { case (src, equalsTo) => Includes(src, equalsTo) }

  val oneOfReader: ConfigReader[OneOf] =
    propertyReader("src", Expression.reader)
      .zip(propertyReader("one-of", Expression.reader))
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
