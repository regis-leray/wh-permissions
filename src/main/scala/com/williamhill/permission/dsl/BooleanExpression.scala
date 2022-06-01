package com.williamhill.permission.dsl

import pureconfig.ConfigReader
import pureconfig.error.{ConfigReaderFailures, ConvertFailure, KeyNotFound}

sealed trait BooleanExpression

object BooleanExpression {

  case class Equals(src: Expression, other: Expression)   extends BooleanExpression
  case class Includes(src: Expression, other: Expression) extends BooleanExpression
  case class OneOf(src: Expression, other: Expression)    extends BooleanExpression
  case class Overlaps(src: Expression, other: Expression) extends BooleanExpression
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

  def twoPropsExpression[A](key1: String, key2: String)(f: (Expression, Expression) => A): ConfigReader[A] =
    propertyReader(key1, Expression.reader).zip(propertyReader(key2, Expression.reader)).map(f.tupled)

  val equalsReader: ConfigReader[Equals]             = twoPropsExpression("src", "equals")(Equals)
  val includesReader: ConfigReader[Includes]         = twoPropsExpression("src", "includes")(Includes)
  val oneOfReader: ConfigReader[OneOf]               = twoPropsExpression("src", "one-of")(OneOf)
  val intersectsReader: ConfigReader[Overlaps]       = twoPropsExpression("src", "overlaps")(Overlaps)
  val definedReader: ConfigReader[BooleanExpression] = propertyReader("defined", Expression.JsonPath.reader.map(Defined))
  def andReader: ConfigReader[BooleanExpression]     = propertyReader("all", ConfigReader[List[BooleanExpression]].map(And))
  def orReader: ConfigReader[BooleanExpression]      = propertyReader("any", ConfigReader[List[BooleanExpression]].map(Or))

  implicit def reader: ConfigReader[BooleanExpression] =
    equalsReader
      .orElse(definedReader)
      .orElse(includesReader)
      .orElse(oneOfReader)
      .orElse(intersectsReader)
      .orElse(andReader)
      .orElse(orReader)
}
