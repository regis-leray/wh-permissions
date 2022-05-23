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

  case class Single[+T](
      value: MappingValue[T],
      when: Option[BooleanExpression] = None,
      defaultTo: Option[Single[T]] = None,
  ) extends MappingExpression[T]

  object Single {
    implicit def reader[T](implicit mv: MappingValue.Reader[T]): ConfigReader[Single[T]] = {
      mv.map(Single(_, None, None))
        .orElse(ConfigReader.fromCursor { cursor =>
          for {
            obj <- cursor.asMap
            value <- obj.get("value") match {
              case Some(valueCursor) => mv.from(valueCursor)
              case None              => Left(ConfigReaderFailures(ConvertFailure(KeyNotFound("value", obj.keys.toSet), cursor)))
            }
            when    <- obj.get("when").traverse(BooleanExpression.reader.from)
            default <- obj.get("default-to").traverse(reader.from)
          } yield Single(value, when, default)
        })
    }
  }

  case class Multiple[+T](expressions: List[Single[T]]) extends MappingExpression[T]

  object Multiple {
    implicit def reader[T: ConfigReader]: ConfigReader[Multiple[T]] =
      ConfigReader[List[Single[T]]].map(Multiple(_))
  }

  implicit def reader[T: ConfigReader]: ConfigReader[MappingExpression[T]] =
    Multiple.reader[T].orElse(Single.reader[T])

}
