package com.williamhill.permission.dsl

import io.circe.parser.parse as parseJson
import io.circe.syntax.EncoderOps
import io.circe.{Encoder, Json}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import pureconfig.ConfigSource

case class Scenario private (
    hint: String,
    json: String,
    config: String,
    expectedOutput: Json,
)

object Scenario {
  def apply[A: Encoder](hint: String, json: String, expr: String, expectedOutput: A): Scenario =
    new Scenario(hint, json, expr, expectedOutput.asJson)
}

abstract class ExpressionEvaluationTester(scenarios: Scenario*) extends AnyFreeSpec with Matchers with TableDrivenPropertyChecks {

  forAll(Table("Scenario", scenarios*)) { case Scenario(hint, jsonString, exprString, expectedOutput) =>
    hint in {
      val json = parseJson(jsonString).getOrElse(fail(s"invalid json: $jsonString"))
      val expr = ConfigSource.string(exprString).load[Expression].getOrElse(fail(s"invalid expression: $exprString"))
      new ExpressionEvaluator(json.hcursor).evaluateJson(expr) shouldBe expectedOutput
    }
  }

}
