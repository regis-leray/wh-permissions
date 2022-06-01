package com.williamhill.permission.dsl

class ConditionalAnySpec
    extends ExpressionEvaluationTester(
      Scenario(
        hint = "one expression returns true",
        json = """{"foo": "bar"}""",
        expr = """
          |value = true
          |when = {
          |  any = [
          |    { defined = "$.foo" }
          |  ]
          |}""".stripMargin,
        expectedOutput = true,
      ),
      Scenario(
        hint = "one expression returns false",
        json = """{"foo": "bar"}""",
        expr = """
          |value = true
          |when = {
          |  any = [
          |    { defined = "$.xxx" }
          |  ]
          |}""".stripMargin,
        expectedOutput = None,
      ),
      Scenario(
        hint = "two expressions returning true",
        json = """{"foo": "bar"}""",
        expr = """
          |value = true
          |when = {
          |  any = [
          |    { defined = "$.foo" },
          |    { src = "$.foo", equals = "bar" }
          |  ]
          |}""".stripMargin,
        expectedOutput = true,
      ),
      Scenario(
        hint = "two expressions returning true and false",
        json = """{"foo": "bar"}""",
        expr = """
          |value = true
          |when = {
          |  any = [
          |    { defined = "$.foo" },
          |    { defined = "$.xxx" }
          |  ]
          |}""".stripMargin,
        expectedOutput = true,
      ),
      Scenario(
        hint = "two expressions returning false",
        json = """{"foo": "bar"}""",
        expr = """
          |value = true
          |when = {
          |  any = [
          |    { defined = "$.yyy" },
          |    { defined = "$.xxx" }
          |  ]
          |}""".stripMargin,
        expectedOutput = None,
      ),
    )
