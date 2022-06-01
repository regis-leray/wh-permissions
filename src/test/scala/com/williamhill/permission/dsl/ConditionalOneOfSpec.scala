package com.williamhill.permission.dsl

class ConditionalOneOfSpec
    extends ExpressionEvaluationTester(
      Scenario(
        hint = "Constant is one of JSON array",
        json = "[1, 2, 1936, 3]",
        expr = """{ value = true, when = { src = 1936, one-of = "$.*" } }""",
        expectedOutput = true,
      ),
      Scenario(
        hint = "Constant is NOT one of JSON array",
        json = "[1, 2, 3]",
        expr = """{ value = true, when = { src = 1936, one-of = "$.*" } }""",
        expectedOutput = None,
      ),
      Scenario(
        hint = "JSON number is one of constants",
        json = """{"foo": 1936}""",
        expr = """{ value = true, when = { src = "$.foo", one-of = [1, 2, 1936, 3] } }""",
        expectedOutput = true,
      ),
      Scenario(
        hint = "JSON number is NOT one of constants",
        json = """{"foo": 1936}""",
        expr = """{ value = true, when = { src = "$.foo", one-of = [1, 2, 3] } }""",
        expectedOutput = None,
      ),
      Scenario(
        hint = "[1, 2] is NOT one of [1, 2, 3]",
        json = "{}",
        expr = """{ value = true, when = { src = [1, 2], one-of = [1, 2, 3] } }""",
        expectedOutput = None,
      ),
    )
