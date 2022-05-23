package com.williamhill.permission.application.config.dsl

import cats.syntax.traverse.*
import io.circe.Json
import pureconfig.ConfigReader
import pureconfig.error.{ConfigReaderFailures, ConvertFailure, KeyNotFound}

sealed trait MappingExpression[+T]

trait JsonReader {
  implicit val jsonReader: ConfigReader[Json] = ConfigReader.fromCursor { cursor =>
    cursor.asInt
      .map(Json.fromInt)
      .orElse(cursor.asLong.map(Json.fromLong))
      .orElse(cursor.asDouble.map(Json.fromDoubleOrString))
      .orElse(cursor.asBoolean.map(Json.fromBoolean))
      .orElse(cursor.asString.map(Json.fromString))
  }
}

object MappingExpression extends JsonReader {

  sealed trait Single[+T] extends MappingExpression[T] {
    def defaultTo: Option[Single[T]]
  }

  case class Simple[+T](value: MappingValue[T], defaultTo: Option[Single[T]]) extends Single[T]

  object Simple {
    implicit def reader[T](implicit mv: MappingValue.Reader[T]): ConfigReader[Simple[T]] = {
      mv.map(Simple(_, None))
        .orElse(
          ConfigReader.fromCursor { cursor =>
            for {
              obj         <- cursor.asMap
              valueCursor <- obj.get("value").toRight(ConfigReaderFailures(ConvertFailure(KeyNotFound("value", obj.keys.toSet), cursor)))
              value       <- mv.from(valueCursor)
              defaultTo   <- obj.get("default-to").traverse(reader.from)
            } yield Simple(value, defaultTo)
          },
        )
    }
  }

  sealed trait Conditional[+T] extends Single[T] {
    def value: MappingValue[T]
    def defaultTo: Option[Single[T]]
  }

  object Conditional {
    case class WhenEquals[+T](
        value: MappingValue[T],
        whenEquals: List[MappingValue[Json]],
        defaultTo: Option[Single[T]],
    ) extends Conditional[T]

    case class WhenDefined[+T](
        value: MappingValue[T],
        whenDefined: MappingValue.Path,
        defaultTo: Option[Single[T]],
    ) extends Conditional[T]

    private def readerFor[T: ConfigReader](
        key: String,
        getReader: (MappingValue[T], Option[Single[T]]) => ConfigReader[Conditional[T]],
    ): ConfigReader[Conditional[T]] = {
      ConfigReader.fromCursor(cursor =>
        for {
          obj         <- cursor.asMap
          valueCursor <- obj.get("value").toRight(ConfigReaderFailures(ConvertFailure(KeyNotFound("value", obj.keys.toSet), cursor)))
          dataCursor  <- obj.get(key).toRight(ConfigReaderFailures(ConvertFailure(KeyNotFound(key, obj.keys.toSet), cursor)))
          value       <- MappingValue.reader[T].from(valueCursor)
          defaultTo   <- obj.get("default-to").traverse(Single.reader[T].from)
          expression  <- getReader(value, defaultTo).from(dataCursor)
        } yield expression,
      )
    }

    private def whenEqualsReader[T: ConfigReader]: ConfigReader[Conditional[T]] =
      readerFor(
        key = "when-equals",
        getReader = { case (value, defaultTo) =>
          ConfigReader
            .fromCursor(cursor =>
              ConfigReader[List[MappingValue[Json]]]
                .from(cursor)
                .map(WhenEquals(value, _, defaultTo)),
            )
        },
      )

    private def whenDefinedReader[T: ConfigReader]: ConfigReader[Conditional[T]] =
      readerFor(
        key = "when-defined",
        getReader = { case (value, defaultTo) =>
          ConfigReader
            .fromCursor(cursor =>
              MappingValue.Path.reader
                .from(cursor)
                .map(WhenDefined(value, _, defaultTo)),
            )
        },
      )

    implicit def reader[T: ConfigReader]: ConfigReader[Conditional[T]] =
      whenEqualsReader[T].orElse(whenDefinedReader[T])
  }

  object Single {
    implicit def reader[T: ConfigReader]: ConfigReader[Single[T]] =
      Conditional.reader[T].orElse(Simple.reader[T])
  }

  case class Multiple[+T](expressions: List[Single[T]]) extends MappingExpression[T]

  object Multiple {
    implicit def reader[T: ConfigReader]: ConfigReader[Multiple[T]] =
      ConfigReader[List[Single[T]]].map(Multiple(_))
  }

  implicit def reader[T: ConfigReader]: ConfigReader[MappingExpression[T]] =
    Multiple.reader[T].orElse(Single.reader[T])

}
