package com.wh.permission.rule.dsl

import cats.data.NonEmptySet
import com.wh.permission.rule.dsl.Expr.Export.*
import com.wh.permission.rule.dsl.Permission.{Permissions, denyAll}
import com.wh.permission.rule.dsl.errors.RuleError.InvalidAccountIdPath
import io.circe.Json
import io.circe.optics.JsonPath
import io.circe.parser.parse
import zio.test.*
import zio.test.Assertion.*
import zio.test.environment.TestEnvironment

object RuleSpec extends DefaultRunnableSpec {

  def permRule(rule1: Expr[Json, Boolean], path: JsonPath) = new PermissionRule("PermTestRule") {
    override val rule: Expr[Json, Boolean]         = rule1
    override val accountId: JsonPath               = path
    override val permissions: (Facet, Permissions) = Facet.Payment -> denyAll
  }

  override def spec: ZSpec[TestEnvironment, Any] = suite("Rule Spec")(run1Spec + runSpec)

  private def run1Spec = {
    testM("run1 should return some value") {
      val input = parse(s"""{  "account" : { "id" : "123" } } """).toOption.get
      val prule = permRule(string($.account.id) === "123", $.account.id)
      assertM(Rules.run1(input)(prule))(isSome)
    } + testM("run1 should return None") {
      val input = parse(s"""{  "account" : { "id" : "123" } } """).toOption.get
      val prule = permRule(string($.account.wrong) === "123", $.account.id)
      assertM(Rules.run1(input)(prule))(isNone)
    } + testM("run1 should return None Value if rule is false") {
      val input = parse(s"""{  "account" : { "id" : "123" } } """).toOption.get
      val prule = permRule(string($.account.id) === "wrong", $.account.id)
      assertM(Rules.run1(input)(prule))(isNone)
    } + testM("run1 should return None Value if rule is false & Account id missing") {
      val input = parse(s"""{  "account" : { "id" : "123" } } """).toOption.get
      val prule = permRule(string($.account.id) === "wrong", $.account.wrong)
      assertM(Rules.run1(input)(prule))(isNone)
    } + testM("run1 should return an error if AccountId is missing") {
      val input = parse(s"""{  "account" : { "id" : "123" } } """).toOption.get
      val prule = permRule(string($.account.id) === "123", $.account.wrong)
      assertM(Rules.run1(input)(prule).run)(fails(isSubtype[InvalidAccountIdPath](anything)))
    }
  }

  private def runSpec = {
    testM("run should return list value ") {
      val input = parse(s"""{  "account" : { "id" : "123" } } """).toOption.get
      val prule = permRule(string($.account.id) === "123", $.account.id)

      assertM(Rules.run(input)(NonEmptySet.of(prule)))(isNonEmpty)
    } + testM("run should return empty value if field rule not found") {
      val input = parse(s"""{  "account" : { "id" : "123" } } """).toOption.get
      val prule = permRule(string($.account.wrong) === "123", $.account.id)

      assertM(Rules.run(input)(NonEmptySet.of(prule)))(isEmpty)
    } + testM("run should return empty value if value not found") {
      val input = parse(s"""{  "account" : { "id" : "123" } } """).toOption.get
      val prule = permRule(string($.account.id) === "3", $.account.id)

      assertM(Rules.run(input)(NonEmptySet.of(prule)))(isEmpty)
    } + testM("run should return error if AccountId is Missing") {
      val input = parse(s"""{  "account" : { "id" : "123" } } """).toOption.get
      val prule = permRule(string($.account.id) === "123", $.account.wrong)

      assertM(Rules.run(input)(NonEmptySet.of(prule)).run)(fails(isSubtype[InvalidAccountIdPath](anything)))
    }
  }

}
