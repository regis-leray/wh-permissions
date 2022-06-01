package com.williamhill.permission.dsl

class ConditionalEqualsSpec
    extends ExpressionEvaluationTester(
      Scenario(
        hint = "JSON path matches expected value",
        json = """{"qux": 1936}""",
        expr = """{ value: true, when: { src = "$.qux", equals = 1936 } }""",
        expectedOutput = true,
      ),
      Scenario(
        hint = "JSON path does not match expected value (same type)",
        json = """{"qux": 1936}""",
        expr = """{ value: true, when: { src = "$.qux", equals = 1937 } }""",
        expectedOutput = None,
      ),
      Scenario(
        hint = "JSON path does not matches expected value (different types)",
        json = """{"qux": 1936}""",
        expr = """{ value: true, when: { src = "$.qux", equals = "wow" } }""",
        expectedOutput = None,
      ),
      Scenario(
        hint = "JSON path matches expected value (implicit cast from String to Int)",
        json = """{"qux": 1936}""",
        expr = """{ value: true, when: { src = "$.qux", equals = "1936" } }""",
        expectedOutput = true,
      ),
      Scenario(
        hint = "JSON path is missing",
        json = """{}""",
        expr = """{ value: true, when: { src = "$.qux", equals = 1936 } }""",
        expectedOutput = None,
      ),
      Scenario(
        hint = "matching all elements in array",
        json = """[{"qux": 1936}, {"qux": 1936}]""",
        expr = """{ value: true, when: { src = "$.*.qux", equals = 1936 } }""",
        expectedOutput = true,
      ),
      Scenario(
        hint = "matching all EXISTING elements in array",
        json = """[{"qux": 1936}, {"foo": "bar"}]""",
        expr = """{ value: true, when: { src = "$.*.qux", equals = 1936 } }""",
        expectedOutput = true,
      ),
      Scenario(
        hint = "matching only some elements in array",
        json = """[{"qux": 1936}, {"qux": 1937}]""",
        expr = """{ value: true, when: { src = "$.*.qux", equals = 1936 } }""",
        expectedOutput = None,
      ),
      Scenario(
        hint = "matching nothing (empty array)",
        json = """[{"foo": 1936}, {"foo": 1937}]""",
        expr = """{ value: true, when: { src = "$.*.qux", equals = 1936 } }""",
        expectedOutput = None,
      ),
      Scenario(
        hint = "comparing 2 identical arrays",
        json = """[{"qux": 1936}, {"qux": 1937}]""",
        expr = """{ value: true, when: { src = "$.*.qux", equals = [1936, 1937] } }""",
        expectedOutput = true,
      ),
      Scenario(
        hint = "comparing 2 arrays with the same elements in different order",
        json = """[{"qux": 1936}, {"qux": 1937}]""",
        expr = """{ value: true, when: { src = "$.*.qux", equals = [1937, 1936] } }""",
        expectedOutput = true,
      ),
      Scenario(
        hint = "comparing 2 sets with the same elements",
        json = """[{"qux": 1936}, {"qux": 1937}, {"qux": 1936}]""",
        expr = """{ value: true, when: { src = "$.*.qux", equals = [1937, 1936] } }""",
        expectedOutput = true,
      ),
    )
