package com.williamhill.permission.dsl

import com.williamhill.permission.dsl.Expression.Conditional
import io.circe.Json
import io.circe.parser.parse as parseJson
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.{TableDrivenPropertyChecks, TableFor2, TableFor3}
import pureconfig.ConfigSource

class ConditionalExpressionEvaluationSpec extends AnyFreeSpec with Matchers with TableDrivenPropertyChecks {

  val json: Json =
    parseJson(
      """
         |{
         | "v": "found",
         | "foo": [{"qux": 1936}, {"qux": 1936}],
         | "bar": [{"qux": 1937}, {"qux": 1936}],
         | "baz": [{"qux": 1936}, {"zzz": 0}],
         | "qux": 1936
         |}
         |""".stripMargin,
    ).getOrElse(fail("Invalid JSON"))

  type TableType = TableFor3[String, String, Boolean]
  private val tableHeader: (String, String, String) = ("hint", "when", "has results")

  val equalsScenarios: TableType = Table(
    tableHeader,
    (
      "JSON path matches expected value",
      """{ src = "$.qux", equals = 1936 }""".stripMargin,
      true,
    ),
    (
      "JSON path does not match expected value",
      """{ src = "$.qux", equals = 1937 }""".stripMargin,
      false,
    ),
    (
      "JSON path contains a different type",
      """{ src = "$.qux", equals = "1937" }""".stripMargin,
      false,
    ),
    (
      "JSON path is missing",
      """{ src = "$.nothing.here[0]", equals = "1937" }""".stripMargin,
      false,
    ),
    (
      "partial matching in array (wildcard syntax)",
      """{ src = "$.bar.*.qux", equals = 1936 }""",
      false,
    ),
    (
      "matching nothing in array (wildcard syntax)",
      """{ src = "$.foo.*.qux", equals = 1937 }""",
      false,
    ),
    (
      "matching all in array (wildcard syntax)",
      """{ src = "$.bar.*.qux", equals = [1936, 1937] }""",
      true,
    ),
    (
      "matching array element .[N]",
      """{ src: "$.bar[0].qux", equals: 1937 }""",
      true,
    ),
    (
      "not matching array element .[N]",
      """{ src: "$.bar[1].qux", equals: 1937 }""",
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
    (
      "all elements in array are defined .*",
      """{ defined: "$.foo.*.qux" }""",
      true,
    ),
    (
      "no elements in array are defined .*",
      """{ defined: "$.foo.*.zzz" }""",
      false,
    ),
    (
      "only one element in array is defined .*",
      """{ defined: "$.baz.*.zzz" }""",
      false,
    ),
  )

  val andScenarios: TableType = Table(
    tableHeader,
    (
      "0 clauses will always be true",
      """{ all: [] }""",
      true,
    ),
    (
      "1 clause (true)",
      """{ all: [ { defined: "$.qux" } ] }""",
      true,
    ),
    (
      "1 clause (false)",
      """{ all: [ { defined: "$.zzz" } ] }""",
      false,
    ),
    (
      "2 clauses (true)",
      """{ all: [ 
        | { defined: "$.qux" },
        | { src = "$.qux", equals: 1936 }
        |] }""".stripMargin,
      true,
    ),
    (
      "2 clauses (false)",
      """{ all: [ 
        | { defined: "$.qux" },
        | { src = "$.qux", equals: 1937 }
        |] }""".stripMargin,
      false,
    ),
  )

  val orScenarios: TableType = Table(
    tableHeader,
    (
      "0 clauses will always be false",
      """{ any: [] }""",
      false,
    ),
    (
      "1 clause (true)",
      """{ any: [ { defined: "$.qux" } ] }""",
      true,
    ),
    (
      "1 clause (false)",
      """{ any: [ { defined: "$.zzz" } ] }""",
      false,
    ),
    (
      "2 clauses (true)",
      """{ any: [ 
        |  { defined: "$.qux" },
        |  { defined: "$.zzz" }
        |] }""".stripMargin,
      true,
    ),
    (
      "2 clauses (false)",
      """{ any: [ 
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
            .loadOrThrow[Conditional[String]]
          new ExpressionEvaluator(json.hcursor).evaluateFirst(expr) shouldBe Right(
            Option.when(hasResults)("found"),
          )
        }
      }
    }
  }
}
