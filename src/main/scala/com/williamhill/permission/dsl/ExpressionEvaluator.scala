package com.williamhill.permission.dsl

import cats.syntax.traverse.*
import com.williamhill.permission.dsl.Expression.{Conditional, Const, Expressions, JsonPath}
import com.williamhill.permission.dsl.SeqSyntax.SeqExt
import io.circe.{ACursor, Decoder, DecodingFailure}

class ExpressionEvaluator(cursor: ACursor) {

  def mapCursor(f: ACursor => ACursor): ExpressionEvaluator = new ExpressionEvaluator(f(cursor))

  def optional: Option[ExpressionEvaluator] = cursor.focus.map(j => new ExpressionEvaluator(j.hcursor))

  def evaluateAll[T: Decoder](mapping: Expression[T]): Either[DecodingFailure, Vector[T]] =
    mapping match {
      case Conditional(value, when, defaultTo) =>
        for {
          unfiltered   <- evaluateWhen(when)
          values       <- if (unfiltered) evaluateAll(value) else Right(Vector.empty)
          withFallback <- if (values.isEmpty) defaultTo.toVector.flatTraverse(evaluateAll[T]) else Right(values)
        } yield withFallback

      case jsonPath: JsonPath => cursor.downPath(jsonPath).traverse(_.as[Option[T]]).map(_.flatten)

      case Const(value) => Right(Vector(value))

      case Expressions(expressions) => expressions.flatTraverse(evaluateAll[T])
    }

  def evaluateFirst[T: Decoder](mapping: Expression[T]): Either[DecodingFailure, Option[T]] =
    mapping match {
      case Conditional(value, when, defaultTo) =>
        for {
          unfiltered   <- evaluateWhen(when)
          values       <- if (unfiltered) evaluateFirst[T](value) else Right(None)
          withFallback <- if (values.isEmpty) defaultTo.flatTraverse(evaluateFirst[T]) else Right(values)
        } yield withFallback

      case jsonPath: JsonPath => cursor.downPath(jsonPath).traverseSome(_.as[Option[T]])

      case Const(value) => Right(Some(value))

      case Expressions(expressions) => expressions.traverseSome(evaluateFirst[T])
    }

  def evaluateRequired[T: Decoder](mapping: Expression[T]): Either[DecodingFailure, T] =
    evaluateFirst(mapping).flatMap(_.toRight(DecodingFailure(s"Evaluation of $mapping yielded no results", cursor.history)))

  private def evaluateWhen[T](when: Option[BooleanExpression]) =
    when.fold[Either[DecodingFailure, Boolean]](Right(true))(evaluateBoolean)

  private def evaluateBoolean(condition: BooleanExpression): Either[DecodingFailure, Boolean] = condition match {
    case BooleanExpression.And(expressions) => expressions.traverse(evaluateBoolean).map(_.forall(identity))
    case BooleanExpression.Or(expressions)  => expressions.traverse(evaluateBoolean).map(_.exists(identity))

    case BooleanExpression.Equals(src, other) =>
      for {
        a <- evaluateAll(src)
        b <- evaluateAll(other)
      } yield a.toSet == b.toSet

    case BooleanExpression.Includes(src, other) =>
      for {
        a <- evaluateAll(src)
        b <- evaluateAll(other)
      } yield b.forall(a.contains)

    case BooleanExpression.Defined(path) =>
      Right(cursor.downPath(path).forall(_.focus.isDefined))
  }

  implicit class CursorExt(cursor: ACursor) {
    def downPath(path: JsonPath): Vector[ACursor] = {
      path match {
        case JsonPath.NonEmpty(JsonPath.Property(key), tail) => cursor.downField(key).downPath(tail)

        case JsonPath.NonEmpty(JsonPath.ArrayElement(index), tail) =>
          // note: could use downN here, but it doesn't handle negative indexes
          downPath(JsonPath.NonEmpty(JsonPath.ArrayRange(Some(index), Some(index)), tail))

        case JsonPath.NonEmpty(JsonPath.Wildcard, tail) =>
          cursor.focus
            .flatMap(cursor => cursor.asArray.orElse(cursor.asObject.map(_.values.toVector)))
            .toVector
            .flatMap(_.flatMap(_.hcursor.downPath(tail)))

        case JsonPath.NonEmpty(JsonPath.ArrayRange(from, to), tail) =>
          cursor.focus
            .flatMap(_.asArray)
            .toVector
            .flatMap { arr =>
              def indexFor(n: Int) = n match {
                case negative if negative < 0 => arr.length + negative
                case positive                 => positive
              }

              val actualFrom = from.map(indexFor).getOrElse(0)
              val actualTo   = to.map(indexFor).getOrElse(arr.length)

              val cursors =
                if (actualFrom > actualTo) arr.slice(actualTo, actualFrom + 1).reverse
                else arr.slice(actualFrom, actualTo + 1)

              cursors.flatMap(_.hcursor.downPath(tail))
            }

        case JsonPath.Empty => Vector(cursor)
      }
    }
  }

}
