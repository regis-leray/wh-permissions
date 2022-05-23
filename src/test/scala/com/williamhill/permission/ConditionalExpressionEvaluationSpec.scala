package com.williamhill.permission

import com.williamhill.permission.application.config.dsl.MappingExpression.Single
import com.williamhill.permission.utils.JsonSyntax
import io.circe.Json
import io.circe.parser.parse as parseJson
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.{TableDrivenPropertyChecks, TableFor2, TableFor3}
import pureconfig.ConfigSource

class ConditionalExpressionEvaluationSpec extends AnyFreeSpec with Matchers with TableDrivenPropertyChecks with JsonSyntax {

  val json: Json =
    parseJson(
      """
         |{
         | "v": "found",
         | "qux": 1936
         |}
         |""".stripMargin,
    ).getOrElse(fail("Invalid JSON"))

  type TableType = TableFor3[String, String, Boolean]
  private val tableHeader: (String, String, String) = ("hint", "when", "has results")

  val equalsScenarios: TableType = Table(
    tableHeader,
    (
      "1 parameter will always be true",
      """{ equals: ["$.qux"] }""".stripMargin,
      true,
    ),
    (
      "0 parameters will always be false",
      """{ equals: [] }""".stripMargin,
      false,
    ),
    (
      "2 parameters (true)",
      """{ equals: ["$.qux", 1936] }""".stripMargin,
      true,
    ),
    (
      "2 parameters (false)",
      """{ equals: ["$.qux", 1937] }""".stripMargin,
      false,
    ),
    (
      "3 parameters (true)",
      """{ equals: ["$.qux", "$.qux", 1936] }""".stripMargin,
      true,
    ),
    (
      "3 parameters (false)",
      """{ equals: ["$.qux", "$.qux", 1937] }""".stripMargin,
      false,
    ),
  )

  val definedScenarios: TableType = Table(
    tableHeader,
    (
      "defined",
      """{ defined: "$.qux" }""",
      true,
    ),
    (
      "undefined",
      """{ defined: "$.zzz" }""",
      false,
    ),
  )

  val andScenarios: TableType = Table(
    tableHeader,
    (
      "0 clauses will always be true",
      """{ and: [] }""",
      true,
    ),
    (
      "1 clause (true)",
      """{ and: [ { defined: "$.qux" } ] }""",
      true,
    ),
    (
      "1 clause (false)",
      """{ and: [ { defined: "$.zzz" } ] }""",
      false,
    ),
    (
      "2 clauses (true)",
      """{ and: [ 
        | { defined: "$.qux" },
        | { equals: ["$.qux", 1936] }
        |] }""".stripMargin,
      true,
    ),
    (
      "2 clauses (false)",
      """{ and: [ 
        | { defined: "$.qux" },
        | { equals: ["$.qux", 1937] }
        |] }""".stripMargin,
      false,
    ),
  )

  val orScenarios: TableType = Table(
    tableHeader,
    (
      "0 clauses will always be false",
      """{ or: [] }""",
      false,
    ),
    (
      "1 clause (true)",
      """{ or: [ { defined: "$.qux" } ] }""",
      true,
    ),
    (
      "1 clause (false)",
      """{ or: [ { defined: "$.zzz" } ] }""",
      false,
    ),
    (
      "2 clauses (true)",
      """{ or: [ 
        |  { defined: "$.qux" },
        |  { defined: "$.zzz" }
        |] }""".stripMargin,
      true,
    ),
    (
      "2 clauses (false)",
      """{ or: [ 
        |  { defined: "$.yyy" },
        |  { defined: "$.zzz" }
        |] }""".stripMargin,
      false,
    ),
  )

  val scenarios: TableFor2[String, TableType] = Table(
    ("type", "sub-scenarios"),
    ("equals", equalsScenarios),
    ("defined", definedScenarios),
    ("and", andScenarios),
    ("or", orScenarios),
  )

  forAll(scenarios) { case (name, scenarios) =>
    name - {
      forAll(scenarios) { case (hint, when, hasResults) =>
        hint in {
          val expr = ConfigSource
            .string(s"""
                |value = "$$.v"
                |when = $when
                |""".stripMargin)
            .loadOrThrow[Single[String]]
          json.hcursor.evaluateOption(expr) shouldBe Right(Option.when(hasResults)("found"))
        }
      }
    }
  }
}
