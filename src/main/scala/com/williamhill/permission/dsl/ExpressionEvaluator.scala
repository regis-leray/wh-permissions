package com.williamhill.permission.dsl

import cats.syntax.traverse.toTraverseOps
import com.williamhill.permission.dsl.Expression.{Conditional, Const, Expressions, JsonPath}
import io.circe.{ACursor, Decoder, DecodingFailure, Json}

class ExpressionEvaluator(cursor: ACursor) {

  def mapCursor(f: ACursor => ACursor): ExpressionEvaluator = new ExpressionEvaluator(f(cursor))

  def optional: Option[ExpressionEvaluator] = cursor.focus.map(j => new ExpressionEvaluator(j.hcursor))

  def evaluateJson(mapping: Expression): Json =
    mapping match {
      case Conditional(value, when, defaultTo) =>
        Option
          .when(evaluateWhen(when))(evaluateJson(value))
          .filterNot(_.isNull)
          .orElse(defaultTo.map(evaluateJson))
          .getOrElse(Json.Null)

      case jsonPath: JsonPath => JsonPathEvaluator.evaluate(cursor, jsonPath).asJson

      case const @ Const(_) => const.value

      case Expressions(expressions) => Json.arr(expressions.map(evaluateJson)*)
    }

  def evaluateJsonArr(mapping: Expression): Vector[Json] =
    mapping match {
      case Conditional(value, when, defaultTo) =>
        if (evaluateWhen(when)) evaluateJsonArr(value)
        else defaultTo.map(evaluateJsonArr).getOrElse(Vector.empty)

      case jsonPath: JsonPath => JsonPathEvaluator.evaluate(cursor, jsonPath).asJsonArr

      case const @ Const(_) => Vector(const.value)

      case Expressions(expressions) => expressions.map(evaluateJson)
    }

  def evaluate[A: Decoder](mapping: Expression): Either[DecodingFailure, A] =
    evaluateJson(mapping).as[A]

  def evaluateVector[A: Decoder](mapping: Expression): Either[DecodingFailure, Vector[A]] = {
    val json = evaluateJson(mapping)
    json.asArray
      .map(_.traverse(_.as[A]))
      .getOrElse(json.as[Option[A]].map(_.toVector))
  }

  private def evaluateWhen(when: Option[BooleanExpression]): Boolean = when.forall(evaluateBoolean)

  private def evaluateBoolean(condition: BooleanExpression): Boolean = condition match {
    case BooleanExpression.And(expressions)     => expressions.map(evaluateBoolean).forall(identity)
    case BooleanExpression.Or(expressions)      => expressions.map(evaluateBoolean).exists(identity)
    case BooleanExpression.Equals(src, other)   => evaluateJsonArr(src).toSet == evaluateJsonArr(other).toSet
    case BooleanExpression.Includes(src, other) => evaluateJsonArr(src).contains(evaluateJson(other))
    case BooleanExpression.OneOf(src, other)    => evaluateJsonArr(other).contains(evaluateJson(src))
    case BooleanExpression.Overlaps(src, other) => evaluateJsonArr(src).toSet.intersect(evaluateJsonArr(other).toSet).nonEmpty
    case BooleanExpression.Defined(path)        => evaluateJsonArr(path).nonEmpty
  }

}
