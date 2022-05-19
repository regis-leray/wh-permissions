package com.williamhill.permission.utils

import cats.syntax.traverse.*
import com.williamhill.permission.application.config.{MappingExpression, MappingValue}
import io.circe.{ACursor, Decoder, DecodingFailure}

trait JsonSyntax {

  implicit class CursorExt(cursor: ACursor) {
    def downPath(path: MappingValue.Path): ACursor =
      path.path.split('.').toList.foldLeft(cursor)(_ downField _)

    def findFirst[T](options: List[MappingValue.Path])(implicit decoder: Decoder[Option[T]]): Either[DecodingFailure, Option[T]] =
      options match {
        case Nil => Right(None)
        case head :: tail =>
          cursor.downPath(head).as[Option[T]].flatMap {
            case None        => findFirst(tail)
            case Some(found) => Right(Some(found))
          }
      }

    def evaluate(mapping: MappingValue): Either[DecodingFailure, String] = mapping match {
      case path: MappingValue.Path       => downPath(path).as[String]
      case MappingValue.Hardcoded(value) => Right(value)
    }

    def evaluate(mapping: MappingExpression): Either[DecodingFailure, List[String]] = {
      mapping match {
        case MappingExpression.SimpleExpression(value) =>
          evaluate(value).map(List(_))

        case MappingExpression.ConditionalExpression.WhenEquals(value, toCompare) =>
          for {
            cmpValues <- toCompare.traverse(cursor.evaluate(_))
            result    <- if (cmpValues.distinct.size == 1) cursor.evaluate(value).map(List(_)) else Right(Nil)
          } yield result

        case MappingExpression.ConditionalExpression.WhenDefined(value, x) =>
          cursor.downPath(x).focus match {
            case Some(_) => cursor.evaluate(value).map(List(_))
            case None    => Right(Nil)
          }

        case MappingExpression.ExpressionList(expressions) =>
          expressions.flatTraverse(expression => cursor.evaluate(expression))
      }
    }
  }

}

object JsonSyntax extends JsonSyntax
