package com.williamhill.permission.dsl

import cats.syntax.traverse.*
import pureconfig.ConfigReader
import pureconfig.error.{ConfigReaderFailures, ConvertFailure, KeyNotFound}

sealed trait Expression[+T]

object Expression {

  case class Basic[+T](
      value: Value[T],
      when: Option[BooleanExpression] = None,
      defaultTo: Option[Basic[T]] = None,
  ) extends Expression[T]

  object Basic {
    implicit def reader[T](implicit mv: Value.Reader[T]): ConfigReader[Basic[T]] = {
      mv.map(Basic(_))
        .orElse(ConfigReader.fromCursor { cursor =>
          for {
            obj <- cursor.asMap
            value <- obj.get("value") match {
              case Some(valueCursor) => mv.from(valueCursor)
              case None              => Left(ConfigReaderFailures(ConvertFailure(KeyNotFound("value", obj.keys.toSet), cursor)))
            }
            when    <- obj.get("when").traverse(BooleanExpression.reader.from)
            default <- obj.get("default-to").traverse(reader.from)
          } yield Basic(value, when, default)
        })
    }
  }

  case class Bucket[+T](expressions: List[Basic[T]]) extends Expression[T]

  object Bucket {
    implicit def reader[T: ConfigReader]: ConfigReader[Bucket[T]] =
      ConfigReader[List[Basic[T]]].map(Bucket(_))
  }

  implicit def reader[T: ConfigReader]: ConfigReader[Expression[T]] =
    Bucket.reader[T].orElse(Basic.reader[T])

}
