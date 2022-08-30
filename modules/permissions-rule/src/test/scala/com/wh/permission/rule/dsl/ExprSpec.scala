package com.wh.permission.rule.dsl

import com.wh.permission.rule.dsl
import com.wh.permission.rule.dsl.Expr.Export.*
import com.wh.permission.rule.dsl.errors.ParsingError
import io.circe.Json
import io.circe.parser.parse
import zio.{Cause, Chunk, ZIO}
import zio.clock.Clock
import zio.test.{DefaultRunnableSpec, Gen, assertTrue, checkM, failed}

import java.time.LocalDate

object ExprSpec extends DefaultRunnableSpec {

  val isStaff   = boolean($.account.isStaff)
  val isSenior  = boolean($.account.isSenior)
  val name      = string($.account.name)
  val age       = int($.account.age)
  val birthDate = date($.account.birthDate)
  val createdAt = instant($.account.createdAt)
  val roles     = strings($.account.roles)

  private val assertFalse = (b: Boolean) => assertTrue(!b)

  def spec = suite("Rule dsl")(
    isTrueSpec + notEqualSpec + equalSpec + listNotEqualSpec + listEqualSpec + lowerCaseSpec + listNotEqualSpec + allOfSpec +
      lowerOrEqualThanSpec + greaterOrEqualThanSpec + lowerThanSpec + greaterThanSpec + yearSpec + subtractSpec + notSpec + orSpec + andSpec
      + lengthSpec + oneOfSPec + matchingSpec + betweenSpec,
  )

  def validate(rule: dsl.Expr[_, Boolean], input: Json): ZIO[Clock, ParsingError, Boolean] =
    Runtime.run(rule)(input)

  private def isTrueSpec =
    testM("isTrue should return TRUE if value is equal to TRUE") {
      checkM(Gen.boolean) { bool =>
        val expected = bool == true
        val rule     = isStaff.isTrue === expected
        val input    = parse(s"""{  "account" : { "isStaff" : $bool } } """).toOption.get

        validate(rule, input).map(assertTrue(_))
      }
    }

  private def notEqualSpec =
    testM("<> should return TRUE if left value is not equal to right value") {
      checkM(Gen.anyInt, Gen.anyInt) { (left, right) =>
        val expected = left != right
        val rule     = age <> right === expected
        val input    = parse(s"""{  "account" : { "age" : $left } } """).toOption.get
        validate(rule, input).map(assertTrue(_))
      }
    }

  private def equalSpec =
    testM("=== should return TRUE if left value is equal to right value") {
      checkM(Gen.anyInt, Gen.anyInt) { (left, right) =>
        val expected = left == right
        val rule     = age === right === expected

        val input = parse(s"""{  "account" : { "age" : $left } } """).toOption.get
        validate(rule, input).map(assertTrue(_))
      }
    }

  private def listNotEqualSpec =
    testM("<> should return TRUE if left collection is not equal to right collection") {
      checkM(Gen.listOf(Gen.anyString), Gen.listOf(Gen.anyString)) { (left, right) =>
        val expected = left != right
        val rule     = roles <> right === expected

        val v     = Json.fromValues(left.map(Json.fromString))
        val input = parse(s"""{  "account" : { "roles" : ${v.noSpaces} } } """).toOption.get

        validate(rule, input).map(assertTrue(_))
      }
    }

  private def listEqualSpec =
    testM("=== should return TRUE if left collection is equal to right collection") {
      checkM(Gen.listOf(Gen.anyString), Gen.listOf(Gen.anyString)) { (left, right) =>
        val expected = left == right
        val rule     = roles === right === expected

        val v     = Json.fromValues(left.map(Json.fromString))
        val input = parse(s"""{  "account" : { "roles" : ${v.noSpaces} } } """).toOption.get

        validate(rule, input).map(assertTrue(_))
      }
    }

  private def lowerCaseSpec =
    testM("lowercase should return TRUE if left value is same lower case to right value") {
      checkM(Gen.anyString, Gen.anyString) { (left, right) =>
        val expected = left.toLowerCase == right.toLowerCase
        val rule     = name.lowercase === right.toLowerCase === expected

        val input = parse(s"""{  "account" : { "name" : "$left" } } """).toOption.get
        validate(rule, input).map(assertTrue(_))
      }
    }

  private def lowerOrEqualThanSpec =
    testM("<= should return TRUE if left value is smaller or equal to right value") {
      checkM(Gen.anyInt, Gen.anyInt) { (left, right) =>
        val expected = left <= right
        val rule     = age <= right === expected
        val input    = parse(s"""{  "account" : { "age" : $left } } """).toOption.get
        validate(rule, input).map(assertTrue(_))
      }
    }

  private def greaterOrEqualThanSpec =
    testM(">= should return TRUE if left value is greater or equal to right value") {
      checkM(Gen.anyInt, Gen.anyInt) { (left, right) =>
        val expected = left >= right
        val rule     = age >= right === expected
        val input    = parse(s"""{  "account" : { "age" : $left } } """).toOption.get
        validate(rule, input).map(assertTrue(_))
      }
    }

  private def lowerThanSpec =
    testM("< should return TRUE if left value is lower than right value") {
      checkM(Gen.anyInt, Gen.anyInt) { (left, right) =>
        val expected = left < right
        val rule     = age < right === expected
        val input    = parse(s"""{  "account" : { "age" : $left } } """).toOption.get
        validate(rule, input).map(assertTrue(_))
      }
    }

  private def greaterThanSpec =
    testM("> should return TRUE if left value is greater than right value") {
      checkM(Gen.anyInt, Gen.anyInt) { (left, right) =>
        val expected = left > right
        val rule     = age > right === expected
        val input    = parse(s"""{  "account" : { "age" : $left } } """).toOption.get
        validate(rule, input).map(assertTrue(_))
      }
    }

  private def yearSpec =
    testM("year should return the year of a given date") {
      checkM(Gen.anyLocalDate) { date =>
        val expected = date.getYear()
        val rule     = birthDate.year === expected
        val input    = parse(s"""{  "account" : { "birthDate" : "$date" } } """).toOption.get
        validate(rule, input).map(assertTrue(_))
      }
    }

  private def subtractSpec =
    testM("- should the difference between two integers") {
      checkM(Gen.int(0, 5), Gen.int(0, 5)) { (left, right) =>
        val expected = left - right
        val rule     = age - right === expected
        val input    = parse(s"""{  "account" : { "age" : $left } } """).toOption.get
        validate(rule, input).map(assertTrue(_))
      }
    }

  private def notSpec =
    testM("! should true if the proposition is false") {
      checkM(Gen.boolean) { bool =>
        val expected = !bool
        val rule     = !isStaff
        val input    = parse(s"""{  "account" : { "isStaff" : $bool } } """).toOption.get
        validate(rule, input).map(res => assertTrue(res == expected))
      }
    }

  private def orSpec =
    testM("|| should true if at least one proposition is true") {
      checkM(Gen.boolean, Gen.boolean) { (left, right) =>
        val expected = left || right
        val rule     = isStaff || isSenior
        val input    = parse(s"""{  "account" : { "isStaff" : $left, "isSenior": $right } } """).toOption.get
        validate(rule, input).map(res => assertTrue(res == expected))
      }
    }

  private def andSpec =
    testM("&& should true if both propositions are true") {
      checkM(Gen.boolean, Gen.boolean) { (left, right) =>
        val expected = left && right
        val rule     = isStaff && isSenior
        val input    = parse(s"""{  "account" : { "isStaff" : $left, "isSenior": $right } } """).toOption.get
        validate(rule, input).map(res => assertTrue(res == expected))
      }
    }

  private def lengthSpec =
    testM("length should return input size") {
      checkM(Gen.anyString) { name0 =>
        val expected = name0.length()
        val rule     = name.length === expected
        val input    = parse(s"""{  "account" : { "name" : "${name0.mkString}" } } """).toOption.get
        validate(rule, input).map(assertTrue(_))
      }
    }

  private def oneOfSPec =
    testM("oneOf should return TRUE if the expression belongs to the enum") {
      checkM(Gen.listOfBounded(1, 3)(Gen.anyString)) {
        case first :: others =>
          val rule  = name.oneOf(first, others.map(literal(_)): _*)
          val input = parse(s"""{  "account" : { "name" : "$first" } } """).toOption.get
          validate(rule, input).map(assertTrue(_))
        case _ => failed(Cause.fail(Chunk("Programming error.")))
      }
    } + testM("oneOf should return FALSE if the expression does not belong to the enum") {
      checkM(Gen.listOfBounded(1, 3)(Gen.anyString)) {
        case first :: others =>
          val rule  = name.oneOf(first, others.map(literal(_)): _*)
          val input = parse(s"""{  "account" : { "name" : "random" } } """).toOption.get
          validate(rule, input).map(assertFalse(_))
        case _ => failed(Cause.fail(Chunk("Programming error.")))
      }
    } + testM("oneOf should return TRUE if the expression does not belong to the list") {
      checkM(Gen.listOfBounded(1, 3)(Gen.anyString)) {
        case first :: others =>
          val rule  = roles.oneOf(first, others.map(literal(_)): _*)
          val input = parse(s"""{  "account" : { "roles" : ["$first"] } } """).toOption.get
          validate(rule, input).map(assertTrue(_))
        case _ => failed(Cause.fail(Chunk("Programming error.")))
      }
    } + testM("oneOf should return FALSE if the expression does not belong to the list") {
      checkM(Gen.listOfBounded(1, 3)(Gen.anyString)) {
        case first :: others =>
          val rule  = roles.oneOf(first, others.map(literal(_)): _*)
          val input = parse(s"""{  "account" : { "roles" : ["random"] } } """).toOption.get
          validate(rule, input).map(assertFalse(_))
        case _ => failed(Cause.fail(Chunk("Programming error.")))
      }
    }

  private def allOfSpec =
    testM("allOf should return TRUE if the expression belongs to the enum") {
      checkM(Gen.listOfBounded(1, 3)(Gen.anyString)) {
        case first :: _ =>
          val rule  = name.allOf(first)
          val input = parse(s"""{  "account" : { "name" : "$first" } } """).toOption.get
          validate(rule, input).map(assertTrue(_))
        case _ => failed(Cause.fail(Chunk("Programming error.")))
      }
    } + testM("allOf should return FALSE if the expression does not belong to the enum") {
      checkM(Gen.listOfBounded(1, 3)(Gen.anyString)) {
        case first :: others =>
          val rule  = name.allOf(first, others.map(literal(_)): _*)
          val input = parse(s"""{  "account" : { "name" : "random" } } """).toOption.get
          validate(rule, input).map(assertFalse(_))
        case _ => failed(Cause.fail(Chunk("Programming error.")))
      }
    } + testM("allOf should return TRUE if the expression does not belong to the list") {
      checkM(Gen.listOfBounded(1, 3)(Gen.anyString)) {
        case first :: others =>
          val rule  = roles.allOf(first, others.map(literal(_)): _*)
          val v     = Json.fromValues((first :: others).map(Json.fromString))
          val input = parse(s"""{  "account" : { "roles" : ${v.noSpaces} } } """).toOption.get
          validate(rule, input).map(assertTrue(_))
        case _ => failed(Cause.fail(Chunk("Programming error.")))
      }
    } + testM("allOf should return FALSE if the expression does not belong to the list") {
      checkM(Gen.listOfBounded(1, 3)(Gen.anyString)) {
        case first :: others =>
          val rule  = roles.allOf(first, others.map(literal(_)): _*)
          val input = parse(s"""{  "account" : { "roles" : ["random"] } } """).toOption.get
          validate(rule, input).map(assertFalse(_))
        case _ => failed(Cause.fail(Chunk("Programming error.")))
      }
    }

  private def matchingSpec =
    testM("matching should return TRUE when input is as expected") {
      checkM(Gen.listOfN(5)(Gen.alphaChar)) { name0 =>
        val rule  = name.matching("""[aA-zZ]{1,5}""".r)
        val input = parse(s"""{  "account" : { "name" : "${name0.mkString}" } } """).toOption.get
        validate(rule, input).map(assertTrue(_))
      }
    } +
      testM("matching should return FALSE when input is as expected") {
        checkM(Gen.listOfN(6)(Gen.alphaChar)) { name0 =>
          val rule  = name.matching("""[aA-zZ]{1,5}""".r)
          val input = parse(s"""{  "account" : { "name" : "${name0.mkString}" } } """).toOption.get
          validate(rule, input).map(res => assertTrue(!res))
        }
      }

  private def betweenSpec = {
    implicit val localDateOrdering: Ordering[LocalDate] =
      Ordering.by(_.toEpochDay)

    testM(s"between should return TRUE if age is within bounds") {
      checkM(Gen.int(0, 5)) { bound =>
        val rule  = age.between(bound - 1, bound + 1)
        val input = parse(s""" { "account" : { "age" : $bound }  } """).toOption.get
        validate(rule, input).map(assertTrue(_))
      }
    } + testM(s"between should return TRUE if age is equal to low bound") {
      checkM(Gen.int(0, 5)) { bound =>
        val rule  = age.between(bound, bound + 1)
        val input = parse(s""" { "account" : { "age" : $bound }  } """).toOption.get
        validate(rule, input).map(assertTrue(_))
      }
    } + testM(s"between should return TRUE if age is equal to high bound") {
      checkM(Gen.int(0, 5)) { bound =>
        val rule  = age.between(bound - 1, bound)
        val input = parse(s""" { "account" : { "age" : $bound }  } """).toOption.get
        validate(rule, input).map(assertTrue(_))
      }
    } + testM(s"between should return FALSE if age is lower than low bound") {
      checkM(Gen.int(0, 5)) { bound =>
        val rule  = age.between(bound, bound + 1)
        val input = parse(s""" { "account" : { "age" : ${bound - 1} }  } """).toOption.get
        validate(rule, input).map(assertFalse(_))
      }
    } + testM(s"between should return FALSE if age is greater than high bound") {
      checkM(Gen.int(0, 5)) { bound =>
        val rule  = age.between(bound - 1, bound)
        val input = parse(s""" { "account" : { "age" : ${bound + 1} }  } """).toOption.get
        validate(rule, input).map(assertFalse(_))
      }
    } + testM(s"between should return TRUE if birthDate is within bounds") {
      checkM(Gen.anyLocalDate) { bound =>
        val rule  = birthDate.between(bound.minusDays(1), bound.plusDays(1))
        val input = parse(s""" { "account" : { "birthDate" : "$bound" }  } """).toOption.get
        validate(rule, input).map(assertTrue(_))
      }
    } + testM(s"between should return TRUE if birthDate is equal to low bound") {
      checkM(Gen.anyLocalDate) { bound =>
        val rule  = birthDate.between(bound, bound.plusDays(1))
        val input = parse(s""" { "account" : { "birthDate" : "$bound" }  } """).toOption.get
        validate(rule, input).map(assertTrue(_))
      }
    } + testM(s"between should return TRUE if birthDate is equal to high bound") {
      checkM(Gen.anyLocalDate) { bound =>
        val rule  = birthDate.between(bound.minusDays(1), bound)
        val input = parse(s""" { "account" : { "birthDate" : "$bound" }  } """).toOption.get
        validate(rule, input).map(assertTrue(_))
      }
    } + testM(s"between should return FALSE if birthDate is lower than low bound") {
      checkM(Gen.anyLocalDate) { bound =>
        val rule  = birthDate.between(bound, bound.plusDays(1))
        val input = parse(s""" { "account" : { "birthDate" : "${bound.minusDays(1)}" }  } """).toOption.get
        validate(rule, input).map(assertFalse(_))
      }
    } + testM(s"between should return FALSE if birthDate is greater than high bound") {
      checkM(Gen.anyLocalDate) { bound =>
        val rule  = birthDate.between(bound.minusDays(1), bound)
        val input = parse(s""" { "account" : { "birthDate" : "${bound.plusDays(1)}" }  } """).toOption.get
        validate(rule, input).map(assertFalse(_))
      }
    }
  }
}
