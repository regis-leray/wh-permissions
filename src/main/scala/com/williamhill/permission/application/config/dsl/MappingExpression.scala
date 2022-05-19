package com.williamhill.permission.application.config.dsl

import pureconfig.ConfigReader
import pureconfig.error.{ConfigReaderFailures, ConvertFailure, KeyNotFound}

sealed trait MappingExpression[T]

object MappingExpression {

  sealed trait Single[T] extends MappingExpression[T] {
    def value: MappingValue[T]
  }

  case class Simple[T](value: MappingValue[T]) extends Single[T]

  object Simple {
    implicit def reader[T: ConfigReader]: ConfigReader[Simple[T]] =
      MappingValue.reader[T].map(Simple(_))
  }

  sealed trait Conditional[T] extends Single[T]

  object Conditional {
    case class WhenEquals[T](value: MappingValue[T], whenEquals: List[MappingValue[T]]) extends Conditional[T]
    case class WhenDefined[T](value: MappingValue[T], whenDefined: MappingValue.Path)   extends Conditional[T]

    private def readerFor[T](
        key: String,
        getReader: MappingValue[T] => ConfigReader[Conditional[T]],
    )(implicit mv: MappingValue.Reader[T]): ConfigReader[Conditional[T]] = {
      ConfigReader.fromCursor(cursor =>
        for {
          obj         <- cursor.asMap
          valueCursor <- obj.get("value").toRight(ConfigReaderFailures(ConvertFailure(KeyNotFound("value", obj.keys.toSet), cursor)))
          dataCursor  <- obj.get(key).toRight(ConfigReaderFailures(ConvertFailure(KeyNotFound(key, obj.keys.toSet), cursor)))
          value       <- mv.from(valueCursor)
          expression  <- getReader(value).from(dataCursor)
        } yield expression,
      )
    }

    private def whenEqualsReader[T: MappingValue.Reader]: ConfigReader[Conditional[T]] =
      readerFor(
        key = "when-equals",
        getReader = value => ConfigReader.fromCursor(cursor => ConfigReader[List[MappingValue[T]]].from(cursor).map(WhenEquals(value, _))),
      )

    private def whenDefinedReader[T: MappingValue.Reader]: ConfigReader[Conditional[T]] =
      readerFor(
        key = "when-defined",
        getReader = value => ConfigReader.fromCursor(cursor => MappingValue.Path.reader.from(cursor).map(WhenDefined(value, _))),
      )

    implicit def reader[T: MappingValue.Reader]: ConfigReader[Conditional[T]] =
      whenEqualsReader.orElse(whenDefinedReader)
  }

  object Single {
    implicit def reader[T: ConfigReader]: ConfigReader[Single[T]] =
      Conditional.reader[T].orElse(Simple.reader[T])
  }

  case class Multiple[T](expressions: List[Single[T]]) extends MappingExpression[T]

  object Multiple {
    implicit def reader[T: ConfigReader]: ConfigReader[Multiple[T]] =
      ConfigReader[List[Single[T]]].map(Multiple(_))
  }

  implicit def reader[T: ConfigReader]: ConfigReader[MappingExpression[T]] =
    Multiple.reader[T].orElse(Single.reader[T])

}
