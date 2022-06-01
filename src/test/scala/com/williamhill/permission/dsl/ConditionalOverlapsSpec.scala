package com.williamhill.permission.dsl

class ConditionalOverlapsSpec
    extends ExpressionEvaluationTester(
      Scenario(
        hint = "Two lists are the same",
        json = "{}",
        expr = """{ value: true, when: { src = [1, 2], overlaps = [1, 2] } }""",
        expectedOutput = true,
      ),
      Scenario(
        hint = "every element of src is contained in the other list",
        json = "{}",
        expr = """{ value: true, when: { src = [1, 2], overlaps = [1, 2, 3] } }""",
        expectedOutput = true,
      ),
      Scenario(
        hint = "every element of the other list is contained in src",
        json = "{}",
        expr = """{ value: true, when: { src = [1, 2, 3], overlaps = [1, 2] } }""",
        expectedOutput = true,
      ),
      Scenario(
        hint = "a single element is evaluated as a list",
        json = "{}",
        expr = """{ value: true, when: { src = 1, overlaps = [1, 2] } }""",
        expectedOutput = true,
      ),
      Scenario(
        hint = "some common elements between the two lists",
        json = "{}",
        expr = """{ value: true, when: { src = [1, 2], overlaps = [2, 3] } }""",
        expectedOutput = true,
      ),
      Scenario(
        hint = "two lists have nothing in common",
        json = "{}",
        expr = """{ value: true, when: { src = [1, 2], overlaps = [3, 4] } }""",
        expectedOutput = None,
      ),
      Scenario(
        hint = "src element not in list",
        json = "{}",
        expr = """{ value: true, when: { src = 1, overlaps = [3, 4] } }""",
        expectedOutput = None,
      ),
    )
