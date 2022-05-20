package com.williamhill.permission.utils

import cats.syntax.traverse.*
import com.williamhill.permission.application.config.dsl.{MappingExpression, MappingValue}
import io.circe.{ACursor, Decoder, DecodingFailure}

trait JsonSyntax {

  type OptionDecoder[T] = Decoder[Option[T]]

  implicit class CursorExt(cursor: ACursor) {
    def downPath(path: MappingValue.Path): ACursor =
      path.path.split('.').toList.foldLeft(cursor)(_ downField _)

    def evaluate[T: Decoder](mapping: MappingValue[T]): Either[DecodingFailure, T] = mapping match {
      case path: MappingValue.Path       => downPath(path).as[T]
      case MappingValue.Hardcoded(value) => Right(value)
    }

    def evaluateList[T: Decoder](mapping: MappingExpression[T]): Either[DecodingFailure, List[T]] = {
      mapping match {
        case single: MappingExpression.Single[T] =>
          evaluateOption(single).map(_.toList)

        case MappingExpression.Multiple(expressions) =>
          expressions.flatTraverse(expression => cursor.evaluateList(expression))
      }
    }

    def evaluateOption[T: OptionDecoder](mapping: MappingExpression.Single[T]): Either[DecodingFailure, Option[T]] = {
      mapping match {
        case MappingExpression.Simple(value, defaultTo) =>
          for {
            a <- evaluate(value.optional)
            b <- defaultTo.filter(_ => a.isEmpty).flatTraverse(evaluateOption(_))
          } yield a.orElse(b)

        case MappingExpression.Conditional.WhenEquals(value, toCompare, defaultTo) =>
          for {
            cmpValues <- toCompare.traverse(v => cursor.evaluate(v.optional))
            result    <- if (cmpValues.distinct.size == 1) cursor.evaluate(value.optional) else defaultTo.flatTraverse(evaluateOption(_))
          } yield result

        case MappingExpression.Conditional.WhenDefined(value, path, defaultTo) =>
          cursor.downPath(path).focus match {
            case Some(_) => cursor.evaluate(value.optional)
            case None    => defaultTo.flatTraverse(cursor.evaluateOption(_))
          }
      }
    }

    def evaluateFirst[T: Decoder](mapping: MappingExpression[T]): Either[DecodingFailure, Option[T]] = {
      mapping match {
        case single: MappingExpression.Single[T] =>
          evaluateOption(single)

        case MappingExpression.Multiple(head :: tail) =>
          evaluateOption(head) match {
            case Right(None) => evaluateFirst(MappingExpression.Multiple(tail))
            case result      => result
          }

        case MappingExpression.Multiple(Nil) => Right(None)
      }
    }
  }

}

object JsonSyntax extends JsonSyntax
