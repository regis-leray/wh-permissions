package com.williamhill.permission

import com.williamhill.permission.application.config.dsl.MappingExpression
import com.williamhill.permission.application.config.dsl.MappingExpression.Conditional.{WhenDefined, WhenEquals}
import com.williamhill.permission.application.config.dsl.MappingExpression.{Multiple, Simple}
import com.williamhill.permission.application.config.dsl.MappingValue.{Hardcoded, Path}
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
      val output = Simple(Path("hello.world"), None)

      test(input, output)
    }

    "Hardcoded" in {
      val input  = """x = "hello.world""""
      val output = Simple(Hardcoded("hello.world"), None)

      test(input, output)
    }

    "With default value (path)" in {
      val input =
        """
          |x = {
          |  value = "$.hello.world"
          |  default-to = "$.something else"
          |}""".stripMargin

      val output = Simple(
        Path("hello.world"),
        Some(Simple(Path("something else"), None)),
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

      val output = Simple(
        Path("hello.world"),
        Some(Simple(Hardcoded("something else"), None)),
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

      val output = Simple(
        Path("foo.bar"),
        Some(
          Simple(
            Path("foo.baz"),
            Some(
              Simple(
                Path("foo.qux"),
                None,
              ),
            ),
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
          |  when-defined = "$.foo.bar"
          |}
          |""".stripMargin

      val output = WhenDefined(
        Path("hello.world"),
        Path("foo.bar"),
        None,
      )

      test(input, output)
    }

    "With default" in {
      val input =
        """
          |x = {
          |  value = "$.hello.world"
          |  when-defined = "$.foo.bar"
          |  default-to: {
          |    value = "$.foo.baz"
          |    when-defined = "$.foo.cux"
          |  }
          |}
          |""".stripMargin

      val output = WhenDefined(
        Path("hello.world"),
        Path("foo.bar"),
        Some(
          WhenDefined(
            Path("foo.baz"),
            Path("foo.cux"),
            None,
          ),
        ),
      )

      test(input, output)
    }

  }

  "When equals" - {

    "Without default" in {
      val input =
        """
          |x = {
          |  value = "$.hello.world"
          |  when-equals = ["$.foo.bar", "BAZ BAZ!"]
          |}
          |""".stripMargin

      val output = WhenEquals(
        Path("hello.world"),
        List(Path("foo.bar"), Hardcoded("BAZ BAZ!")),
        None,
      )

      test(input, output)
    }

    "With default" in {
      val input =
        """
          |x = {
          |  value = "$.hello.world"
          |  when-equals = ["$.foo.bar", "BAZ BAZ!"]
          |  default-to = {
          |    value = "$.cux.cux"
          |    when-equals = ["HELLO", "WORLD"]
          |  }
          |}
          |""".stripMargin

      val output = WhenEquals(
        Path("hello.world"),
        List(Path("foo.bar"), Hardcoded("BAZ BAZ!")),
        Some(
          WhenEquals(
            Path("cux.cux"),
            List(Hardcoded("HELLO"), Hardcoded("WORLD")),
            None,
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
        |    when-defined = "$.foo.bar"
        |    default-to = "yyy"
        |  }
        |]
        |""".stripMargin

    val output = Multiple(
      List(
        Simple(Path("hello"), None),
        Simple(Hardcoded("world"), None),
        WhenDefined(
          Hardcoded("xxx"),
          Path("foo.bar"),
          Some(Simple(Hardcoded("yyy"), None)),
        ),
      ),
    )

    test(input, output)

  }

  private def test(input: String, output: MappingExpression[String]): Assertion =
    ConfigSource.string(input).loadOrThrow[Output] shouldBe Output(output)

}
