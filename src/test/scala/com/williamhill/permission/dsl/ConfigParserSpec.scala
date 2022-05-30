package com.williamhill.permission.dsl

import com.williamhill.permission.dsl.BooleanExpression.{Defined, Equals}
import com.williamhill.permission.dsl.Expression.{Conditional, Const, Expressions, JsonPath}
import io.circe.Json
import org.scalatest.Assertion
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import pureconfig.generic.semiauto.deriveReader
import pureconfig.{ConfigReader, ConfigSource}

class ConfigParserSpec extends AnyFreeSpec with Matchers {

  case class Output(x: Expression[String])
  implicit val reader: ConfigReader[Output] = deriveReader

  "Simple expression" - {

    "Json path" in {
      val input = """x = "$.hello['world'][*][2][-2][3:9][3:][:9][-5:-10].*""""
      val output =
        JsonPath.Property("hello") /:
          JsonPath.Property("world") /:
          JsonPath.Wildcard /:
          JsonPath.ArrayElement(2) /:
          JsonPath.ArrayElement(-2) /:
          JsonPath.ArrayRange(Some(3), Some(9)) /:
          JsonPath.ArrayRange(Some(3), None) /:
          JsonPath.ArrayRange(None, Some(9)) /:
          JsonPath.ArrayRange(Some(-5), Some(-10)) /:
          JsonPath.Wildcard /:
          JsonPath.Empty

      test(input, output)
    }

    "Hardcoded" in {
      val input  = """x = "hello.world""""
      val output = Const("hello.world")

      test(input, output)
    }

    "With default value (path)" in {
      val input =
        """
          |x = {
          |  value = "$.hello.world"
          |  default-to = "$.something.else"
          |}""".stripMargin

      val output = Conditional(
        JsonPath.Property("hello") /: JsonPath.Property("world").toPath,
        defaultTo = Some(JsonPath.Property("something") /: JsonPath.Property("else").toPath),
      )

      test(input, output)
    }

    "With default value (hardcoded)" in {
      val input =
        """
          |x = {
          |  value = "$.hello"
          |  default-to = "something else"
          |}""".stripMargin

      val output = Conditional(
        JsonPath.Property("hello").toPath,
        defaultTo = Some(Const("something else")),
      )

      test(input, output)
    }

    "With nested default values" in {
      val input =
        """
          |x = {
          |  value = "$.foo"
          |  default-to = {
          |    value = "$.bar"
          |    default-to = "$.baz"
          |  }
          |}""".stripMargin

      val output = Conditional(
        JsonPath.Property("foo").toPath,
        defaultTo = Some(
          Conditional(
            JsonPath.Property("bar").toPath,
            defaultTo = Some(JsonPath.Property("baz").toPath),
          ),
        ),
      )

      test(input, output)
    }

  }

  "When defined" - {

    "Without default" in {
      val input =
        """
          |x = {
          |  value = "$.hello"
          |  when = { defined = "$.foo" }
          |}
          |""".stripMargin

      val output = Conditional(
        JsonPath.Property("hello").toPath,
        when = Some(Defined(JsonPath.Property("foo").toPath)),
      )

      test(input, output)
    }

    "With default" in {
      val input =
        """
          |x = {
          |  value = "$.hello"
          |  when = { defined = "$.foo" }
          |  default-to: {
          |    value = "$.bar"
          |    when = { defined = "$.baz" }
          |  }
          |}
          |""".stripMargin

      val output = Conditional(
        JsonPath.Property("hello").toPath,
        when = Some(Defined(JsonPath.Property("foo").toPath)),
        defaultTo = Some(
          Conditional(
            JsonPath.Property("bar").toPath,
            when = Some(Defined(JsonPath.Property("baz").toPath)),
          ),
        ),
      )

      test(input, output)
    }

  }

  "When equals" - {

    "Comparing strings" in {
      val input =
        """
          |x = {
          |  value = "$.hello"
          |  when = { src = "$.foo", equals = "BAZ BAZ!" }
          |}
          |""".stripMargin

      val output = Conditional(
        JsonPath.Property("hello").toPath,
        when = Some(Equals(JsonPath.Property("foo").toPath, Const(Json.fromString("BAZ BAZ!")))),
      )

      test(input, output)
    }

    "Comparing booleans" in {
      val input =
        """
          |x = {
          |  value = "$.hello"
          |  when = { src = "$.foo", equals = true }
          |}
          |""".stripMargin

      val output = Conditional(
        JsonPath.Property("hello").toPath,
        when = Some(Equals(JsonPath.Property("foo").toPath, Const(Json.fromBoolean(true)))),
      )

      test(input, output)
    }

    "Comparing integers" in {
      val input =
        """
          |x = {
          |  value = "$.hello"
          |  when = { src = "$.foo", equals = 99 }
          |}
          |""".stripMargin

      val output = Conditional(
        JsonPath.Property("hello").toPath,
        when = Some(Equals(JsonPath.Property("foo").toPath, Const(Json.fromInt(99)))),
      )

      test(input, output)
    }

    "Comparing longs" in {
      val input =
        """
          |x = {
          |  value = "$.hello"
          |  when = { src = "$.foo", equals = 2147483648 }
          |}
          |""".stripMargin

      val output = Conditional(
        JsonPath.Property("hello").toPath,
        when = Some(Equals(JsonPath.Property("foo").toPath, Const(Json.fromLong(2147483648L)))),
      )

      test(input, output)
    }

    "Comparing doubles" in {
      val input =
        """
          |x = {
          |  value = "$.hello"
          |  when = { src = "$.foo", equals = 99.12 }
          |}
          |""".stripMargin

      val output = Conditional(
        JsonPath.Property("hello").toPath,
        when = Some(Equals(JsonPath.Property("foo").toPath, Const(Json.fromDoubleOrNull(99.12)))),
      )

      test(input, output)
    }

    "With default" in {
      val input =
        """
          |x = {
          |  value = "$.hello"
          |  when = { src = "$.foo", equals = "BAZ BAZ!" }
          |  default-to = {
          |    value = "$.bar"
          |    when = { src = "HELLO", equals = "WORLD" }
          |  }
          |}
          |""".stripMargin

      val output = Conditional(
        JsonPath.Property("hello").toPath,
        when = Some(Equals(JsonPath.Property("foo").toPath, Const(Json.fromString("BAZ BAZ!")))),
        defaultTo = Some(
          Conditional(
            JsonPath.Property("bar").toPath,
            when = Some(Equals(Const(Json.fromString("HELLO")), Const(Json.fromString("WORLD")))),
          ),
        ),
      )

      test(input, output)
    }

  }

  "List of expressions" in {

    val input =
      """
        |x = [
        |  "$.hello",
        |  "world",
        |  {
        |    value = "xxx"
        |    when = { defined = "$.foo.bar" }
        |    default-to = "yyy"
        |  }
        |]
        |""".stripMargin

    val output = Expressions(
      Vector(
        JsonPath.Property("hello").toPath,
        Const("world"),
        Conditional(
          Const("xxx"),
          when = Some(Defined(JsonPath.Property("foo") /: JsonPath.Property("bar").toPath)),
          defaultTo = Some(Const("yyy")),
        ),
      ),
    )

    test(input, output)

  }

  private def test(input: String, output: Expression[String]): Assertion =
    ConfigSource.string(input).loadOrThrow[Output] shouldBe Output(output)

}
