package com.williamhill.permission.utils

import cats.syntax.traverse.*
import com.williamhill.permission.application.config.dsl.{MappingExpression, MappingValue}
import io.circe.{ACursor, Decoder, DecodingFailure}

trait JsonSyntax {

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

    def evaluateOption[T: Decoder](mapping: MappingExpression.Single[T]): Either[DecodingFailure, Option[T]] = {
      mapping match {
        case MappingExpression.Simple(value) =>
          evaluate(value).map(Some(_))

        case MappingExpression.Conditional.WhenEquals(value, toCompare) =>
          for {
            cmpValues <- toCompare.traverse(cursor.evaluate(_))
            result    <- if (cmpValues.distinct.size == 1) cursor.evaluate(value).map(Some(_)) else Right(None)
          } yield result

        case MappingExpression.Conditional.WhenDefined(value, x) =>
          cursor.downPath(x).focus match {
            case Some(_) => cursor.evaluate(value).map(Some(_))
            case None    => Right(None)
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
