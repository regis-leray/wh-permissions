package com.williamhill.permission.dsl

class ConditionalDefaultSpec
    extends ExpressionEvaluationTester(
      Scenario(
        hint = "Default to constant for non-existing JSON property",
        json = """{}""",
        expr = """{ value = "$.foo", default-to = "bar" }""",
        expectedOutput = "bar",
      ),
      Scenario(
        hint = "Default to property for non-existing JSON property",
        json = """{"qux": 123}""",
        expr = """{ value = "$.foo", default-to = "$.qux" }""",
        expectedOutput = 123,
      ),
      Scenario(
        hint = "Default to non-existing JSON property",
        json = """{"qux": 123}""",
        expr = """{ value = "$.foo", default-to = "$.bar" }""",
        expectedOutput = None,
      ),
      Scenario(
        hint = "Default when condition fails",
        json = """{}""",
        expr = """{ value = 1, default-to = 2, when = { src = "a", equals = "b" } }""",
        expectedOutput = 2,
      ),
    )
