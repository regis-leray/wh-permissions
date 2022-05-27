package com.williamhill.permission.dsl

import cats.syntax.traverse.*
import com.williamhill.permission.dsl.Expression.{Basic, Bucket}
import com.williamhill.permission.dsl.SeqSyntax.SeqExt
import com.williamhill.permission.dsl.Value.{Const, JsonPath}
import io.circe.{ACursor, Decoder, DecodingFailure, Json}

object JsonSyntax {

  type OptionDecoder[T] = Decoder[Option[T]]

  implicit class CursorExt(cursor: ACursor) {
    def downPath(path: JsonPath): Vector[ACursor] = {
      path match {
        case JsonPath.NonEmpty(JsonPath.Property(key), tail) => cursor.downField(key).downPath(tail)

        case JsonPath.NonEmpty(JsonPath.ArrayElement(index), tail) => cursor.downN(index).downPath(tail)

        case JsonPath.NonEmpty(JsonPath.Wildcard, tail) =>
          cursor.focus
            .flatMap(cursor => cursor.asArray.orElse(cursor.asObject.map(_.values.toVector)))
            .toVector
            .flatMap(_.flatMap(_.hcursor.downPath(tail)))

        case JsonPath.NonEmpty(JsonPath.ArrayRange(from, to), tail) =>
          cursor.focus
            .flatMap(_.asArray)
            .toVector
            .flatMap(_.slice(from, to).flatMap(_.hcursor.downPath(tail)))

        case JsonPath.Empty => Vector(cursor)
      }
    }

    private def evaluateAll[T: OptionDecoder](mapping: Value[T]): Either[DecodingFailure, Vector[T]] = mapping match {
      case jsonPath: JsonPath => downPath(jsonPath).traverse(_.as[Option[T]]).map(_.flatten)
      case Const(value)       => Right(Vector(value))
    }

    private def evaluateFirst[T: OptionDecoder](mapping: Value[T]): Either[DecodingFailure, Option[T]] = mapping match {
      case jsonPath: JsonPath => downPath(jsonPath).traverseSome(_.as[Option[T]])
      case Const(value)       => Right(Some(value))
    }

    def evaluateAll[T: OptionDecoder](mapping: Expression[T]): Either[DecodingFailure, Vector[T]] =
      mapping match {
        case single: Basic[T] =>
          for {
            unfiltered   <- evaluateWhen(single)
            values       <- if (unfiltered) evaluateAll(single.value) else Right(Vector.empty)
            withFallback <- if (values.isEmpty) single.defaultTo.toVector.flatTraverse(evaluateAll[T]) else Right(values)
          } yield withFallback

        case Bucket(expressions) =>
          expressions.toVector.flatTraverse(evaluateAll[T])
      }

    def evaluateFirst[T: OptionDecoder](mapping: Expression[T]): Either[DecodingFailure, Option[T]] =
      mapping match {
        case single: Basic[T] =>
          for {
            unfiltered   <- evaluateWhen(single)
            values       <- if (unfiltered) evaluateFirst(single.value) else Right(None)
            withFallback <- if (values.isEmpty) single.defaultTo.flatTraverse(evaluateFirst[T]) else Right(values)
          } yield withFallback

        case Bucket(expressions) =>
          expressions.traverseSome(evaluateFirst[T])
      }

    def evaluateRequired[T: OptionDecoder](mapping: Basic[T]): Either[DecodingFailure, T] =
      evaluateFirst(mapping).flatMap(_.toRight(DecodingFailure(s"Evaluation of $mapping yield no results", cursor.history)))

    private def evaluateWhen[T](single: Basic[T]) =
      single.when.fold[Either[DecodingFailure, Boolean]](Right(true))(evaluateBoolean)

    private def evaluateBoolean(condition: BooleanExpression): Either[DecodingFailure, Boolean] = condition match {
      case BooleanExpression.And(expressions) =>
        expressions.traverse(evaluateBoolean).map(_.forall(identity))
      case BooleanExpression.Or(expressions) =>
        expressions.traverse(evaluateBoolean).map(_.exists(identity))
      case BooleanExpression.Equals(values) =>
        values.traverse(evaluateAll[Json]).map(_.flatten.distinct.size == 1)
      case BooleanExpression.Defined(path) =>
        println(cursor.downPath(path))
        Right(cursor.downPath(path).forall(_.focus.isDefined))
    }

  }

}
