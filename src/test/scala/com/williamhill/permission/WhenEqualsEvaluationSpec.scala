package com.williamhill.permission

import com.williamhill.permission.application.config.dsl.MappingExpression.Conditional.WhenEquals
import com.williamhill.permission.application.config.dsl.MappingValue.{Hardcoded, Path}
import com.williamhill.permission.utils.JsonSyntax
import io.circe.Json
import io.circe.parser.parse as parseJson
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.{TableDrivenPropertyChecks, TableFor4}

class WhenEqualsEvaluationSpec extends AnyFreeSpec with Matchers with TableDrivenPropertyChecks with JsonSyntax {

  val scenarios: TableFor4[String, String, WhenEquals[String], Option[String]] = Table(
    ("hint", "json", "config", "expected result"),
    (
      "Extract and compare an integer will yield results",
      """{"name": "foo", "id": 99}""",
      WhenEquals(
        Path("name"),
        List(Path("id"), Hardcoded(Json.fromInt(99))),
        None,
      ),
      Some("foo"),
    ),
    (
      "Extract and compare an integer will yield no results",
      """{"name": "foo", "id": 99}""",
      WhenEquals(
        Path("name"),
        List(Path("id"), Hardcoded(Json.fromInt(123))),
        None,
      ),
      None,
    ),
    (
      "Extract and compare two fields will yield results",
      """{"name": "foo", "id": 99, "anotherId": 99}""",
      WhenEquals(
        Path("name"),
        List(Path("id"), Path("anotherId")),
        None,
      ),
      Some("foo"),
    ),
    (
      "Extract and compare two fields will yield no results",
      """{"name": "foo", "id": 99}""",
      WhenEquals(
        Path("name"),
        List(Path("id"), Path("name")),
        None,
      ),
      None,
    ),
    (
      "Extract and compare three fields will yield results",
      """{"name": "foo", "id": 99, "anotherId": 99}""",
      WhenEquals(
        Path("name"),
        List(Path("id"), Path("anotherId"), Hardcoded(Json.fromInt(99))),
        None,
      ),
      Some("foo"),
    ),
    (
      "Extract and compare three fields will yield no results",
      """{"name": "foo", "id": 99, "anotherId": 99}""",
      WhenEquals(
        Path("name"),
        List(Path("id"), Path("name"), Hardcoded(Json.fromInt(98))),
        None,
      ),
      None,
    ),
  )

  forAll(scenarios) { case (hint, json, config, expectedResult) =>
    hint in {
      val result = parseJson(json)
        .getOrElse(fail(s"Invalid json: $json"))
        .hcursor
        .evaluateOption[String](config)

      result shouldBe Right(expectedResult)
    }
  }
}
