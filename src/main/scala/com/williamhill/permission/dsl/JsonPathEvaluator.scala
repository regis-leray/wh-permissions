package com.williamhill.permission.dsl

import com.williamhill.permission.dsl.Expression.JsonPath
import com.williamhill.permission.dsl.Expression.JsonPath.{ArrayElement, ArrayRange, Property, Wildcard}
import io.circe.ACursor

object JsonPathEvaluator {

  def evaluate(cursor: ACursor, path: JsonPath): JsonEvaluationResult = cursor.evaluate(path)

  implicit class CursorExt(cursor: ACursor) {

    def evaluate(path: JsonPath): JsonEvaluationResult = path match {
      case JsonPath.NonEmpty(Property(key), tail) =>
        cursor.downField(key).evaluate(tail)

      case JsonPath.NonEmpty(Wildcard, tail) =>
        (for {
          c   <- cursor.focus
          arr <- c.asArray.orElse(c.asObject.map(_.values.toVector))
          results = JsonEvaluationResult.Many(arr.map(j => j.hcursor.evaluate(tail)))
        } yield results).getOrElse(JsonEvaluationResult.Empty)

      case JsonPath.NonEmpty(ArrayElement(index), tail) =>
        (for {
          c   <- cursor.focus
          arr <- c.asArray
          realIndex = if (index < 0) arr.length + index else index
          element <-
            try Some(arr.apply(realIndex))
            catch { case _: IndexOutOfBoundsException => None }
        } yield element.hcursor.evaluate(tail)).getOrElse(JsonEvaluationResult.Empty)

      case JsonPath.NonEmpty(ArrayRange(from, to), tail) =>
        (for {
          c   <- cursor.focus
          arr <- c.asArray

          actualFrom = from.map(index => if (index < 0) arr.length + index else index).getOrElse(0)
          actualTo   = to.map(index => if (index < 0) arr.length + index - 1 else index).getOrElse(arr.length)

          children =
            if (actualFrom > actualTo) Vector.empty
            else arr.slice(actualFrom, actualTo + 1)

        } yield JsonEvaluationResult.Many(children.map(j => j.hcursor.evaluate(tail)))).getOrElse(JsonEvaluationResult.Empty)

      case JsonPath.Empty =>
        cursor.focus.fold[JsonEvaluationResult](JsonEvaluationResult.Empty)(JsonEvaluationResult.One)

    }
  }

}
