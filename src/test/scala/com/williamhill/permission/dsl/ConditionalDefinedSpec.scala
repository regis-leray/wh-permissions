package com.williamhill.permission.dsl

class ConditionalDefinedSpec
    extends ExpressionEvaluationTester(
      Scenario(
        hint = "property is defined",
        json = """{"qux": 123}""",
        expr = """{ value = true, when = { defined: "$.qux" } }""",
        expectedOutput = true,
      ),
      Scenario(
        hint = "property not defined",
        json = """{}""",
        expr = """{ value = true, when = { defined: "$.qux" } }""",
        expectedOutput = None,
      ),
      Scenario(
        hint = "nested property is defined",
        json = """{"qux": {"baz": 123}}""",
        expr = """{ value = true, when = { defined: "$.qux.baz" } }""",
        expectedOutput = true,
      ),
      Scenario(
        hint = "nested property not defined",
        json = """{"foo": {}}""",
        expr = """{ value = true, when = { defined: "$.foo.qux" } }""",
        expectedOutput = None,
      ),
      Scenario(
        hint = "property as object is defined",
        json = """{"qux": {"baz": 123}}""",
        expr = """{ value = true, when = { defined: "$.qux" } }""",
        expectedOutput = true,
      ),
      Scenario(
        hint = "array element is defined",
        json = """[123]""",
        expr = """{ value = true, when = { defined: "$[0]" } }""",
        expectedOutput = true,
      ),
      Scenario(
        hint = "array element is not defined",
        json = """[123]""",
        expr = """{ value = true, when = { defined: "$[1]" } }""",
        expectedOutput = None,
      ),
      Scenario(
        hint = "property in array is defined for all elements",
        json = """[{"foo": 1}, {"foo": 2}]""",
        expr = """{ value = true, when = { defined: "$[*].foo" } }""",
        expectedOutput = true,
      ),
      Scenario(
        hint = "property in array is defined at least for some elements",
        json = """[{"foo": 1}, {"bar": 2}]""",
        expr = """{ value = true, when = { defined: "$[*].foo" } }""",
        expectedOutput = true,
      ),
      Scenario(
        hint = "property in array is undefined for all elements",
        json = """[{"foo": 1}, {"bar": 2}]""",
        expr = """{ value = true, when = { defined: "$[*].baz" } }""",
        expectedOutput = None,
      ),
    )
