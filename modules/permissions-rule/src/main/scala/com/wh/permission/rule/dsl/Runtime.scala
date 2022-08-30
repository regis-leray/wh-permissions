package com.wh.permission.rule.dsl

import com.wh.permission.rule.dsl.Expr.*
import com.wh.permission.rule.dsl.FieldType.*
import com.wh.permission.rule.dsl.errors.ParsingError
import com.wh.permission.rule.dsl.errors.ParsingError.{MissingField, TypeMismatch}
import io.circe.Json
import io.circe.optics.JsonPath
import zio.*

import java.time.{Instant, LocalDate}
import scala.util.Try

/** Defines the mechanism to evaluate an expression. It is the equivalent of
  * a compiler in a programming language.
  */
private[dsl] object Runtime {

  // TODO Replace ZIO by Either
  def run[B](expr: Expr[?, B])(rawInput: Json): IO[ParsingError, B] =
    expr match {
      case field @ Field(_)      => parseField(rawInput, field)
      case Literal(value)        => IO.succeed(value)
      case Length(expr)          => run(expr)(rawInput).map(_.length())
      case Matching(expr, regex) => run(expr)(rawInput).map(regex.matches)
      case Not(rule)             => run(rule)(rawInput).map(!_)
      case And(left, right)      => run(left)(rawInput).zipWith(run(right)(rawInput))(_ && _)
      case Now                   => IO.succeed(LocalDate.now())
      case GetYear(expr)         => run(expr)(rawInput).map(_.getYear())
      case LowerCase(expr)       => run(expr)(rawInput).map(_.toLowerCase())
      case Subtract(l, r)        => run(l)(rawInput).zipWith(run(r)(rawInput))(_ - _)
      case IsPresent(path) =>
        path.json.getOption(rawInput) match {
          case Some(_) => IO.succeed(true)
          case _       => IO.fail(MissingField(path, rawInput))
        }
      case OneOf(expr, items) =>
        for {
          set  <- ZIO.foreach(items)(run(_)(rawInput))
          exp1 <- run(expr)(rawInput)
          r = exp1 match {
            case l: List[?] => l.exists(set.contains)
            case elem       => set.contains(elem)
          }
        } yield r

      case AllOf(expr, items) =>
        for {
          set  <- ZIO.foreach(items)(run(_)(rawInput))
          exp1 <- run(expr)(rawInput)
          r = exp1 match {
            case l: List[?] => set.forall(l.contains)
            case elem       => set.contains(elem)
          }
        } yield r

      case IfPresent(field, rule) =>
        parseField(rawInput, field).flatMap {
          case None        => IO.succeed(true)
          case Some(value) => run(rule(value))(rawInput)
        }
      case Relational(l, r, ops) =>
        val left  = run(l)(rawInput)
        val right = run(r)(rawInput)
        val eval  = evalOps(ops)

        left.zipWith(right)(eval)
    }

  private def parseField[B](json: Json, field: Field[_, B]): IO[ParsingError, B] = {
    (field.path.json.getOption(json), field.fieldType) match {
      case (None, Nullable())            => IO.none
      case (Some(value), n @ Nullable()) => parseNotNull(value, n.fieldType, field.path).map(Some(_))
      case (Some(value), NotNull(nn))    => parseNotNull(value, nn, field.path)
      case (Some(value), many @ Many()) =>
        IO.fromOption(value.asArray).orElseFail(arrayExpected(field.path, value)).flatMap { arr =>
          IO.foreach(arr.toList)(a => parseNotNull(a, many.fieldType, field.path))
        }
      case (None, _) => IO.fail(MissingField(field.path, json))
    }
  }

  private def parseNotNull[B](value: Json, fieldType: NotNull[B], fieldPath: JsonPath): IO[ParsingError, B] =
    fieldType match {
      case TString => IO.fromOption(value.asString).orElseFail(stringExpected(fieldPath, value))
      case TInt    => IO.fromOption(value.asNumber.flatMap(_.toInt)).orElseFail(intExpected(fieldPath, value))
      case TBool   => IO.fromOption(value.asBoolean).orElseFail(booleanExpected(fieldPath, value))
      case TDate => IO.fromOption(value.asString.flatMap(d => Try(LocalDate.parse(d)).toOption)).orElseFail(dateExpected(fieldPath, value))
      case TInstant =>
        IO.fromOption(value.asString.flatMap(d => Try(Instant.parse(d)).toOption)).orElseFail(instantExpected(fieldPath, value))
    }

  private def error(expectedType: String)(path: JsonPath, value: Json) =
    TypeMismatch(path, expectedType, value)

  val arrayExpected   = error("List") _
  val stringExpected  = error("String") _
  val intExpected     = error("Int") _
  val booleanExpected = error("Boolean") _
  val dateExpected    = error("Date") _
  val instantExpected = error("Instant") _

  private def evalOps[A](ops: RelationalOps): (A, A) => Boolean =
    ops match {
      case r @ RelationalOps.EQ() => r.equaled.eqv
      case r @ RelationalOps.GT() => r.ordered.gt
      case r @ RelationalOps.LT() => r.ordered.lt
    }
}
