package com.williamhill.permission.utils

import cats.syntax.traverse.*
import com.williamhill.permission.application.config.dsl.{BooleanExpression, MappingExpression, MappingValue}
import io.circe.{ACursor, Decoder, DecodingFailure, Json}

trait JsonSyntax {

  type OptionDecoder[T] = Decoder[Option[T]]

  implicit class CursorExt(cursor: ACursor) {
    def downPath(path: MappingValue.Path): ACursor =
      path.path.split('.').toList.foldLeft(cursor)(_ downField _)

    private def evaluate[T: Decoder](mapping: MappingValue[T]): Either[DecodingFailure, T] = mapping match {
      case path: MappingValue.Path   => downPath(path).as[T]
      case MappingValue.Const(value) => Right(value)
    }

    def evaluateList[T: Decoder](mapping: MappingExpression[T]): Either[DecodingFailure, List[T]] = {
      mapping match {
        case single: MappingExpression.Single[T] =>
          evaluateOption(single).map(_.toList)

        case MappingExpression.Multiple(expressions) =>
          expressions.flatTraverse(expression => cursor.evaluateList(expression))
      }
    }

    def evaluateBoolean(condition: BooleanExpression): Either[DecodingFailure, Boolean] = condition match {
      case BooleanExpression.And(expressions) =>
        expressions.traverse(evaluateBoolean).map(_.forall(identity))
      case BooleanExpression.Or(expressions) =>
        expressions.traverse(evaluateBoolean).map(_.exists(identity))
      case BooleanExpression.Equals(values) =>
        values.traverse(evaluate[Json]).map(_.distinct.size == 1)
      case BooleanExpression.Defined(path) =>
        Right(cursor.downPath(path).focus.isDefined)
    }

    def evaluateOption[T: OptionDecoder](mapping: MappingExpression.Single[T]): Either[DecodingFailure, Option[T]] =
      for {
        unfiltered    <- mapping.when.fold[Either[DecodingFailure, Boolean]](Right(true))(evaluateBoolean)
        optionalValue <- if (unfiltered) evaluate(mapping.value.optional) else Right(None)
        withFallback  <- optionalValue.fold(mapping.defaultTo.flatTraverse(evaluateOption[T]))(v => Right(Some(v)))
      } yield withFallback

    def evaluateRequired[T: OptionDecoder](mapping: MappingExpression.Single[T]): Either[DecodingFailure, T] =
      evaluateOption(mapping).flatMap(_.toRight(DecodingFailure(s"Evaluation of $mapping yield no results", cursor.history)))

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
