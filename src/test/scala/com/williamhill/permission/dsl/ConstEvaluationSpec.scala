package com.williamhill.permission.dsl

class ConstEvaluationSpec
    extends ExpressionEvaluationTester(
      Scenario(
        hint = "Type: integer",
        json = "{}",
        expr = "value = 123",
        expectedOutput = 123,
      ),
      Scenario(
        hint = "Type: long",
        json = "{}",
        expr = "value = 123",
        expectedOutput = 123L,
      ),
      Scenario(
        hint = "Type: double",
        json = "{}",
        expr = "value = 1.23",
        expectedOutput = 1.23,
      ),
      Scenario(
        hint = "Type: string",
        json = "{}",
        expr = """value = "hello"""",
        expectedOutput = "hello",
      ),
      Scenario(
        hint = "Type: list of constants",
        json = "{}",
        expr = "value = [1, 2, 3]",
        expectedOutput = Vector(1, 2, 3),
      ),
    )
