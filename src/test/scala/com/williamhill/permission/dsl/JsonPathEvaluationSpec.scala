package com.williamhill.permission.dsl

import io.circe.parser.parse as parseJson
import io.circe.{Decoder, DecodingFailure}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import pureconfig.ConfigSource

class JsonPathEvaluationSpec extends AnyFreeSpec with Matchers {

  "Evaluation will succeed" - {
    "JSON path (string)" in {
      evaluateAll[String](
        json = """{"hello": "world"}""",
        config = """value = "$.hello"""",
      ) shouldBe Right(Vector("world"))
    }

    "constant (double)" in {
      evaluateAll[Double](
        json = """{}""",
        config = """value = 19.99""",
      ) shouldBe Right(Vector(19.99))
    }

    "missing value" in {
      evaluateAll[String](
        json = """{}""",
        config = """value = "$.hello"""",
      ) shouldBe Right(Vector.empty)
    }

    "querying all elements (wildcard)" in {
      evaluateAll[Int](
        json = """{"hello": [15, 16, 17]}""",
        config = """value = "$.hello.*"""",
      ) shouldBe Right(Vector(15, 16, 17))
    }

    "querying specific element (in range)" in {
      evaluateAll[Int](
        json = """{"hello": [15, 16, 17]}""",
        config = """value = "$.hello[1]"""",
      ) shouldBe Right(Vector(16))
    }

    "querying specific element (out of range)" in {
      evaluateAll[Int](
        json = """{"hello": [15, 16, 17]}""",
        config = """value = "$.hello[4]"""",
      ) shouldBe Right(Vector.empty)
    }

    "querying specific element (negative, in range)" in {
      evaluateAll[Int](
        json = """{"hello": [15, 16, 17]}""",
        config = """value = "$.hello[-1]"""",
      ) shouldBe Right(Vector(17))
    }

    "querying specific element (negative, out of range)" in {
      evaluateAll[Int](
        json = """{"hello": [15, 16, 17]}""",
        config = """value = "$.hello[-4]"""",
      ) shouldBe Right(Vector.empty)
    }

    "querying range" in {
      evaluateAll[Int](
        json = """{"hello": [15, 16, 17, 18]}""",
        config = """value = "$.hello[1:2]"""",
      ) shouldBe Right(Vector(16, 17))
    }

    "querying out of range" in {
      evaluateAll[Int](
        json = """{"hello": [15, 16, 17, 18]}""",
        config = """value = "$.hello[1:10]"""",
      ) shouldBe Right(Vector(16, 17, 18))
    }

    "querying negative range" in {
      evaluateAll[Int](
        json = """{"hello": [15, 16, 17, 18]}""",
        config = """value = "$.hello[-3:-1]"""",
      ) shouldBe Right(Vector(16, 17))
    }

    "querying negative out of range" in {
      evaluateAll[Int](
        json = """{"hello": [15, 16, 17, 18]}""",
        config = """value = "$.hello[-10:-5]"""",
      ) shouldBe Right(Vector.empty)
    }

    "querying nested arrays range" in {
      evaluateAll[Int](
        json = """{"hello": [[1, 2, 3], [4, 5, 6]]}""",
        config = """value = "$.hello[*][1:]"""",
      ) shouldBe Right(Vector(2, 3, 5, 6))
    }

    "wildcard works for objects too" in {
      evaluateAll[Int](
        json = """{"hello": {"foo": [1, 2, 3], "bar": [4, 5, 6]}}""",
        config = """value = "$.hello[*][1:]"""",
      ) shouldBe Right(Vector(2, 3, 5, 6))
    }
  }

  "Evaluation will fail" - {
    "incoherent types" in {
      evaluateAll[String](
        json = """{"hello": 15}""",
        config = """value = "$.hello"""",
      ) shouldBe Left(DecodingFailure("String", Nil))
    }
  }

  private def evaluateAll[T: Decoder](json: String, config: String): Either[DecodingFailure, Vector[T]] = {
    new ExpressionEvaluator(parseJson(json).getOrElse(fail(s"Invalid json: $json")).hcursor).evaluateVector[T](
      ConfigSource.string(config).loadOrThrow[Expression],
    )
  }
}
