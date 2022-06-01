package com.williamhill.permission.dsl

class ConditionalIncludesSpec
    extends ExpressionEvaluationTester(
      Scenario(
        hint = "Constant is included in JSON array",
        json = "[1, 2, 1936, 3]",
        expr = """{ value = true, when = { src = "$.*", includes = 1936 } }""",
        expectedOutput = true,
      ),
      Scenario(
        hint = "Constant is NOT included in JSON array",
        json = "[1, 2, 3]",
        expr = """{ value = true, when = { src = "$.*", includes = 1936 } }""",
        expectedOutput = None,
      ),
      Scenario(
        hint = "JSON number is included in constants",
        json = """{"foo": 1936}""",
        expr = """{ value = true, when = { src = [1, 2, 1936, 3], includes = "$.foo" } }""",
        expectedOutput = true,
      ),
      Scenario(
        hint = "JSON number is NOT included in constants",
        json = """{"foo": 1936}""",
        expr = """{ value = true, when = { src = [1, 2, 3], includes = "$.foo" } }""",
        expectedOutput = None,
      ),
      Scenario(
        hint = "[1, 2] is NOT included in [1, 2, 3]",
        json = "{}",
        expr = """{ value = true, when = { src = [1, 2, 3], includes = [1, 2] } }""",
        expectedOutput = None,
      ),
    )
