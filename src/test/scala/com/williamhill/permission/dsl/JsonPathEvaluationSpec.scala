package com.williamhill.permission.dsl

class JsonPathEvaluationSpec
    extends ExpressionEvaluationTester(
      Scenario(
        hint = "JSON path - property",
        json = """{"hello": "world"}""",
        expr = """value = "$.hello"""",
        expectedOutput = "world",
      ),
      Scenario(
        hint = "missing value",
        json = """{}""",
        expr = """value = "$.hello"""",
        expectedOutput = None,
      ),
      Scenario(
        hint = "querying all elements (wildcard)",
        json = """{"hello": [15, 16, 17]}""",
        expr = """value = "$.hello.*"""",
        expectedOutput = Vector(15, 16, 17),
      ),
      Scenario(
        hint = "querying specific element (in range)",
        json = """{"hello": [15, 16, 17]}""",
        expr = """value = "$.hello[1]"""",
        expectedOutput = 16,
      ),
      Scenario(
        hint = "querying specific element (out of range)",
        json = """{"hello": [15, 16, 17]}""",
        expr = """value = "$.hello[4]"""",
        expectedOutput = None,
      ),
      Scenario(
        hint = "querying specific element (negative, in range)",
        json = """{"hello": [15, 16, 17]}""",
        expr = """value = "$.hello[-1]"""",
        expectedOutput = 17,
      ),
      Scenario(
        hint = "querying specific element (negative, out of range)",
        json = """{"hello": [15, 16, 17]}""",
        expr = """value = "$.hello[-4]"""",
        expectedOutput = None,
      ),
      Scenario(
        hint = "querying range",
        json = """{"hello": [15, 16, 17, 18]}""",
        expr = """value = "$.hello[1:2]"""",
        expectedOutput = Vector(16, 17),
      ),
      Scenario(
        hint = "querying out of range",
        json = """{"hello": [15, 16, 17, 18]}""",
        expr = """value = "$.hello[1:10]"""",
        expectedOutput = Vector(16, 17, 18),
      ),
      Scenario(
        hint = "querying negative range",
        json = """{"hello": [15, 16, 17, 18]}""",
        expr = """value = "$.hello[-3:-1]"""",
        expectedOutput = Vector(16, 17),
      ),
      Scenario(
        hint = "querying negative out of range",
        json = """{"hello": [15, 16, 17, 18]}""",
        expr = """value = "$.hello[-10:-5]"""",
        expectedOutput = Vector.empty[Int],
      ),
      Scenario(
        hint = "querying nested arrays range",
        json = """{"hello": [[1, 2, 3], [4, 5, 6]]}""",
        expr = """value = "$.hello[*][1:]"""",
        expectedOutput = Vector(2, 3, 5, 6),
      ),
      Scenario(
        hint = "wildcard works for objects too",
        json = """{"hello": {"foo": [1, 2, 3], "bar": [4, 5, 6]}}""",
        expr = """value = "$.hello[*][1:]"""",
        expectedOutput = Vector(2, 3, 5, 6),
      ),
    )
