package com.williamhill.permission

import com.williamhill.permission.application.config.dsl.BooleanExpression.{Defined, Equals}
import com.williamhill.permission.application.config.dsl.MappingExpression
import com.williamhill.permission.application.config.dsl.MappingExpression.{Multiple, Single}
import com.williamhill.permission.application.config.dsl.MappingValue.{Const, Path}
import io.circe.Json
import org.scalatest.Assertion
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import pureconfig.generic.semiauto.deriveReader
import pureconfig.{ConfigReader, ConfigSource}

class DslParserSpec extends AnyFreeSpec with Matchers {

  case class Output(x: MappingExpression[String])
  implicit val reader: ConfigReader[Output] = deriveReader

  "Simple expression" - {

    "Path" in {
      val input  = """x = "$.hello.world""""
      val output = Single(Path("hello.world"))

      test(input, output)
    }

    "Hardcoded" in {
      val input  = """x = "hello.world""""
      val output = Single(Const("hello.world"))

      test(input, output)
    }

    "With default value (path)" in {
      val input =
        """
          |x = {
          |  value = "$.hello.world"
          |  default-to = "$.something.else"
          |}""".stripMargin

      val output = Single(
        Path("hello.world"),
        defaultTo = Some(Single(Path("something.else"))),
      )

      test(input, output)
    }

    "With default value (hardcoded)" in {
      val input =
        """
          |x = {
          |  value = "$.hello.world"
          |  default-to = "something else"
          |}""".stripMargin

      val output = Single(
        Path("hello.world"),
        defaultTo = Some(Single(Const("something else"))),
      )

      test(input, output)
    }

    "With nested default values" in {
      val input =
        """
          |x = {
          |  value = "$.foo.bar"
          |  default-to = {
          |    value = "$.foo.baz"
          |    default-to = "$.foo.qux"
          |  }
          |}""".stripMargin

      val output = Single(
        Path("foo.bar"),
        defaultTo = Some(
          Single(
            Path("foo.baz"),
            defaultTo = Some(Single(Path("foo.qux"))),
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
          |  value = "$.hello.world"
          |  when = { defined = "$.foo.bar" }
          |}
          |""".stripMargin

      val output = Single(
        Path("hello.world"),
        when = Some(Defined(Path("foo.bar"))),
      )

      test(input, output)
    }

    "With default" in {
      val input =
        """
          |x = {
          |  value = "$.hello.world"
          |  when = { defined = "$.foo.bar" }
          |  default-to: {
          |    value = "$.foo.baz"
          |    when = { defined = "$.foo.cux" }
          |  }
          |}
          |""".stripMargin

      val output = Single(
        Path("hello.world"),
        when = Some(Defined(Path("foo.bar"))),
        defaultTo = Some(
          Single(
            Path("foo.baz"),
            when = Some(Defined(Path("foo.cux"))),
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
          |  value = "$.hello.world"
          |  when = { equals = ["$.foo.bar", "BAZ BAZ!"] }
          |}
          |""".stripMargin

      val output = Single(
        Path("hello.world"),
        when = Some(Equals(List(Path("foo.bar"), Const(Json.fromString("BAZ BAZ!"))))),
      )

      test(input, output)
    }

    "Comparing booleans" in {
      val input =
        """
          |x = {
          |  value = "$.hello.world"
          |  when = { equals = ["$.foo.bar", true] }
          |}
          |""".stripMargin

      val output = Single(
        Path("hello.world"),
        when = Some(Equals(List(Path("foo.bar"), Const(Json.fromBoolean(true))))),
      )

      test(input, output)
    }

    "Comparing integers" in {
      val input =
        """
          |x = {
          |  value = "$.hello.world"
          |  when = { equals = ["$.foo.bar", 99] }
          |}
          |""".stripMargin

      val output = Single(
        Path("hello.world"),
        when = Some(Equals(List(Path("foo.bar"), Const(Json.fromInt(99))))),
      )

      test(input, output)
    }

    "Comparing longs" in {
      val input =
        """
          |x = {
          |  value = "$.hello.world"
          |  when = { equals = ["$.foo.bar", 2147483648] }
          |}
          |""".stripMargin

      val output = Single(
        Path("hello.world"),
        when = Some(Equals(List(Path("foo.bar"), Const(Json.fromLong(2147483648L))))),
      )

      test(input, output)
    }

    "Comparing doubles" in {
      val input =
        """
          |x = {
          |  value = "$.hello.world"
          |  when = { equals = ["$.foo.bar", 99.12] }
          |}
          |""".stripMargin

      val output = Single(
        Path("hello.world"),
        when = Some(Equals(List(Path("foo.bar"), Const(Json.fromDoubleOrNull(99.12))))),
      )

      test(input, output)
    }

    "With default" in {
      val input =
        """
          |x = {
          |  value = "$.hello.world"
          |  when = { equals = ["$.foo.bar", "BAZ BAZ!"] }
          |  default-to = {
          |    value = "$.cux.cux"
          |    when = { equals = ["HELLO", "WORLD"] }
          |  }
          |}
          |""".stripMargin

      val output = Single(
        Path("hello.world"),
        when = Some(Equals(List(Path("foo.bar"), Const(Json.fromString("BAZ BAZ!"))))),
        defaultTo = Some(
          Single(
            Path("cux.cux"),
            when = Some(Equals(List(Const(Json.fromString("HELLO")), Const(Json.fromString("WORLD"))))),
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

    val output = Multiple(
      List(
        Single(Path("hello"), None),
        Single(Const("world"), None),
        Single(
          Const("xxx"),
          when = Some(Defined(Path("foo.bar"))),
          defaultTo = Some(Single(Const("yyy"))),
        ),
      ),
    )

    test(input, output)

  }

  private def test(input: String, output: MappingExpression[String]): Assertion =
    ConfigSource.string(input).loadOrThrow[Output] shouldBe Output(output)

}
