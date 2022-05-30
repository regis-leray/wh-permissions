package com.williamhill.permission.dsl

import io.circe.{Decoder, DecodingFailure, Json, JsonObject}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import pureconfig.ConfigSource

class ConstEvaluationSpec extends AnyFreeSpec with Matchers {
  "Evaluation of a" - {
    "integer" in { evaluateAll[Int]("value = 123") shouldBe Right(Vector(123)) }
    "long" in { evaluateAll[Long]("value = 123") shouldBe Right(Vector(123L)) }
    "double" in { evaluateAll[Double]("value = 1.23") shouldBe Right(Vector(1.23)) }
    "string" in { evaluateAll[String]("""value = "hello"""") shouldBe Right(Vector("hello")) }
    "list of constants" in { evaluateAll[Int]("value = [1, 2, 3]") shouldBe Right(Vector(1, 2, 3)) }
    "list of list of constants" in { evaluateAll[Int]("value = [1, [2, 3]]") shouldBe Right(Vector(1, 2, 3)) }
  }

  private def evaluateAll[T: Expression.Reader: Decoder](config: String): Either[DecodingFailure, Vector[T]] = {
    new ExpressionEvaluator(Json.fromJsonObject(JsonObject.empty).hcursor).evaluateAll(
      ConfigSource.string(config).loadOrThrow[Expression[T]],
    )
  }
}
