package com.wh.permission.rule.dsl

import io.circe.Json
import io.circe.optics.JsonPath

import java.time.LocalDate
import scala.annotation.nowarn
import scala.util.matching.Regex

/** An expression is the representation of a computation required to validate
  * a rule. It takes a source (`A`) and produces an output (`B`).
  */
sealed trait Expr[-A, +B] { self =>

  /** Combines the current expression (`self`) with another one as long as both output
    * a `Boolean`. The resulting expression will output `true` only if both underlying
    * expression do the same.
    *
    * @param that the expression to combine the current one with
    * @param ev an implicit proof proving the output of the current expression is
    *           a `Boolean`.
    */
  def &&[A0 <: A](that: Expr[A0, Boolean])(implicit ev: B <:< Boolean): Expr[A0, Boolean] =
    Expr.And(self.widen, that)

  /** Combines the current expression (`self`) with another one as long as both output
    * a `Boolean`. The resulting expression will output `true` only if at least
    * one of the underlying expressions does the same.
    *
    * @param that the expression to combine the current one with
    * @param ev an implicit proof proving the output of the current expression is
    *           a `Boolean`.
    */
  def ||[A0 <: A](that: Expr[A0, Boolean])(implicit ev: B <:< Boolean): Expr[A0, Boolean] =
    !(!self.widen && !that) // Thanks to DeMorgan's law

  /** Negate the current expression as long as it is an boolean expression.
    *
    * @param ev an implicit proof proving the output of the current expression is
    *           a `Boolean`.
    */
  def unary_![A0 <: A](implicit ev: B <:< Boolean): Expr[A0, Boolean] =
    self.widen match {
      case Expr.Not(rule) => rule
      case rule           => Expr.Not(rule)
    }

  // Safe cast the output of the current expression to another one.
  // This is safe thanks to the proof provided.
  private def widen[C](implicit ev: B <:< C): Expr[A, C] =
    self.asInstanceOf[Expr[A, C]]

  /** Combines the current expression (`self`) with another one as long as both output
    * an orderable value. The resulting expression will output `true` only if the
    * current expression is considered "greater" than the one it is combined with.
    *
    * @param that the expression to combine the current one with
    * @param ev an implicit proof proving the output of the current expression is
    *           orderable.
    */
  def >[A0 <: A, B0 >: B](that: Expr[A0, B0])(implicit ev: Ordering[B0]): Expr[A0, Boolean] =
    Expr.Relational(self, that, Expr.RelationalOps.GT())

  /** Combines the current expression (`self`) with another one as long as both output
    * an orderable value. The resulting expression will output `true` only if the
    * current expression is considered "greater or equal" than/to the one it is combined with.
    *
    * @param that the expression to combine the current one with
    * @param ev an implicit proof proving the output of the current expression is
    *           orderable.
    */
  def >=[A0 <: A, B0 >: B](that: Expr[A0, B0])(implicit ev: Ordering[B0]): Expr[A0, Boolean] =
    !(self < that)

  /** Combines the current expression (`self`) with another one as long as both output
    * an orderable value. The resulting expression will output `true` only if the
    * current expression is considered "smaller" than the one it is combined with.
    *
    * @param that the expression to combine the current one with
    * @param ev an implicit proof proving the output of the current expression is
    *           orderable.
    */
  def <[A0 <: A, B0 >: B](that: Expr[A0, B0])(implicit ev: Ordering[B0]): Expr[A0, Boolean] =
    Expr.Relational(self, that, Expr.RelationalOps.LT())

  /** Combines the current expression (`self`) with another one as long as both output
    * an orderable value. The resulting expression will output `true` only if the
    * current expression is considered "smaller or equal" than/to the one it is combined with.
    *
    * @param that the expression to combine the current one with
    * @param ev an implicit proof proving the output of the current expression is
    *           orderable.
    */
  def <=[A0 <: A, B0 >: B](that: Expr[A0, B0])(implicit ev: Ordering[B0]): Expr[A0, Boolean] =
    !(self > that)

  /** Combines the current expression (`self`) with another one as long as both output
    * an orderable value. The resulting expression will output `true` only if the
    * current expression is considered "equal" to the one it is combined with.
    *
    * @param that the expression to combine the current one with
    * @param ev an implicit proof proving the output of the current expression is
    *           orderable.
    */
  def ===[A0 <: A, B0 >: B](that: Expr[A0, B0])(implicit ev: Eq[B0]): Expr[A0, Boolean] =
    Expr.Relational(self, that, Expr.RelationalOps.EQ())

  /** Combines the current expression (`self`) with another one as long as both output
    * an orderable value. The resulting expression will output `true` only if the
    * current expression is considered "not equal" to the one it is combined with.
    *
    * @param that the expression to combine the current one with
    * @param ev an implicit proof proving the output of the current expression is
    *           orderable.
    */
  def <>[A0 <: A, B0 >: B](that: Expr[A0, B0])(implicit ev: Eq[B0]): Expr[A0, Boolean] =
    !(self === that)

  /** Checks if the current String expression matches with the given regexp
    *
    * @param regex The regex to match the resulting string with
    * @param ev an implicit proof proving the output of the current expression is
    *           a String.
    */
  def matching[A0 <: A](regex: Regex)(implicit ev: B <:< String): Expr[A0, Boolean] =
    Expr.Matching(self.widen, regex)

  /** Returns the length of the current String expression
    *
    * @param ev an implicit proof proving the output of the current expression is
    *           a String.
    */
  def length(implicit ev: B <:< String): Expr[A, Int] =
    Expr.Length(self.widen)

  /** Converts all of the characters to lower case of the current String expression
    *
    * @param ev an implicit proof proving the output of the current expression is
    *           a String.
    */
  def lowercase(implicit ev: B <:< String): Expr[A, String] =
    Expr.LowerCase(self.widen)

  /** Checks if the current expression is equal to one of the items provided in
    * argument.
    *
    * @param b, bs The items
    */
  def oneOf[A0 <: A, B0 >: B](b: Expr[A0, B0], bs: Expr[A0, B0]*): Expr[A0, Boolean] =
    Expr.OneOf(self.widen, bs.toSet + b)

  /** Checks if the current expression is equal to all of the items provided in
    * argument.
    *
    * @param b, bs The items
    */
  def allOf[A0 <: A, B0 >: B](b: Expr[A0, B0], bs: Expr[A0, B0]*): Expr[A0, Boolean] =
    Expr.AllOf(self.widen, bs.toSet + b)

  /** Checks if the current expression is within the provided inclusive range.
    *
    * @param low the lower bound
    * @param high the higher bound
    * @param ev a proof proving that the current expression's output is orderable
    */
  def between[A0 <: A, B0 >: B](low: B0, high: B0)(implicit ev: Ordering[B0]): Expr[A0, Boolean] =
    self <= Expr.Literal(high) && self >= Expr.Literal(low)

  /** Checks if the output of the current boolean expression is equal to true or not
    *
    * @param ev a proof proving that the current expression's output is a boolean
    */
  def isTrue[A0 <: A](implicit ev: B <:< Boolean): Expr[A0, Boolean] =
    self.widen === Expr.Literal(true)

  /** Returns the year of the current date expression.
    *
    * @param ev a proof proving that the current expression's output is a date
    */
  def year[A0 <: A](implicit ev: B <:< LocalDate): Expr[A0, Int] =
    Expr.GetYear(self.widen)

  /** Returns the difference between the current Int expression and the one
    * provided in argument.
    *
    * @param that the expression to to subtract from the current one
    * @param ev a proof proving the current expression's output is an Int.
    */
  def -[A0 <: A](that: Expr[A0, Int])(implicit ev: B <:< Int): Expr[A0, Int] =
    Expr.Subtract(self.widen, that)
}

object Expr {

  /** A field represents a schema record tagged with a specific configuration.
    * This ensures we do not mismatch fields with unrelated configurations.
    *
    * @param name the name of the field
    * @param record
    */
  case class Field[-A, +B: FieldType](path: JsonPath) extends Expr[A, B] { self =>
    val fieldType: FieldType[B] = implicitly[FieldType[B]]
    val required                = Expr.IsPresent(path)
  }

  type RawField         = Field[_, _]
  type UntaggedField[B] = Field[_, B]

  /** A expression returning whether or not the field name in argument has been
    * provided by the user.
    *
    * @param name the name of the field.
    */
  private[dsl] case class IsPresent[A](path: JsonPath) extends Expr[A, Boolean]

  /** An expression returning the year of an date resulting from the evaluation
    * of the expression passed in argument.
    *
    * @param expr An expression resulting in a local date once evaluated.
    */
  private[dsl] case class GetYear[A](expr: Expr[A, LocalDate]) extends Expr[A, Int]

  private[dsl] case class LowerCase[A](expr: Expr[A, String]) extends Expr[A, String]

  /** An expression returning the difference between two integers resulting by
    * each respective expression passed in parameter.
    *
    * @param left the expression which output should be subtracted
    * @param right the expression which output is subtracted
    */
  private[dsl] case class Subtract[A](left: Expr[A, Int], right: Expr[A, Int]) extends Expr[A, Int]

  /** An expression which output is provided in argument.
    *
    * @param value the value to return once the expression is evaluated
    */
  private[dsl] case class Literal[B](value: B) extends Expr[Any, B]

  /** An expression returning the length of the string output by the underlying
    * expression once evaluated.
    *
    * @param expr the expression to evaluate
    */
  private[dsl] case class Length[A](expr: Expr[A, String]) extends Expr[A, Int]

  /** An expression returning whether or not the provided string expression is
    * matching with the provided regular expression.
    *
    * @param expr the expression which output is matched against the regular expression.
    * @param regex the regular expression.
    */
  private[dsl] case class Matching[A](expr: Expr[A, String], regex: Regex) extends Expr[A, Boolean]

  /** An expression negating the one provided in argument.
    *
    * @param rule the expression to negate.
    */
  private[dsl] case class Not[A](rule: Expr[A, Boolean]) extends Expr[A, Boolean]

  /** An expression returning whether or not the current expression's output
    * belongs to the items provided in argument.
    *
    * @param expr the expressions to evaluate
    * @param items the set of items the expression's output should belong to
    */
  private[dsl] case class OneOf[A, B](expr: Expr[A, B], items: Set[Expr[A, B]]) extends Expr[A, Boolean]

  /** An expression returning whether or not the current expression's output
    * belongs to all items provided in argument.
    *
    * @param expr the expressions to evaluate
    * @param items the set of items the expression's output should belong to
    */
  private[dsl] case class AllOf[A, B](expr: Expr[A, B], items: Set[Expr[A, B]]) extends Expr[A, Boolean]

  /** An expression combining two underlying boolean expressions which output is combined
    * using &&.
    *
    * @param left a expression producing a boolean
    * @param right a expression producing a boolean
    */
  private[dsl] case class And[A](left: Expr[A, Boolean], right: Expr[A, Boolean]) extends Expr[A, Boolean]

  /** An expression producing the current date.
    */
  private[dsl] case object Now extends Expr[Any, LocalDate]

  /** An expression which once evaluated, checks for the presence of a field,
    * and applies a function resulting in a boolean expression if this is the case.
    *
    * This is pretty much a conditional flatMap based on the presence of a
    * field. It is left to the interpreter of this expression to decide what
    * boolean value to return if the field is not present.
    *
    * @param field The field to check
    * @param rule The rule to apply if the field is present.
    */
  private[dsl] case class IfPresent[A, B](
      field: Field[A, Option[B]],
      rule: B => Expr[A, Boolean],
  ) extends Expr[A, Boolean]

  /** An expression describing a relation in terms of ordering between two
    * expression producing comparable values (in the sense of Java).
    *
    * @param left
    * @param right
    * @param ops The operator defining the relation
    */
  private[dsl] case class Relational[A, B](
      left: Expr[A, B],
      right: Expr[A, B],
      ops: RelationalOps,
  ) extends Expr[A, Boolean] {}

  /** The operator used to describe a relation in terms of ordering */
  sealed private[dsl] trait RelationalOps
  private[dsl] object RelationalOps {
    // Equal to
    case class EQ[B: Eq]() extends RelationalOps {
      val equaled = implicitly[Eq[B]]
    }
    // Greater than
    case class GT[B: Ordering]() extends RelationalOps {
      val ordered = implicitly[Ordering[B]]
    }
    // Lower than
    case class LT[B: Ordering]() extends RelationalOps {
      val ordered = implicitly[Ordering[B]]
    }
  }

  /** Regroups all the components that are exported outside the dsl package. */
  object Export extends Export
  trait Export {

    def ifPresent[A, B](expr: Field[A, Option[B]])(f: Expr[A, B] => Expr[A, Boolean]): Expr[A, Boolean] =
      IfPresent(expr, f.compose(Literal.apply[B]))

    def required[A](expr: Field[A, _]): Expr[A, Boolean] =
      IsPresent(expr.path)

    val now: Expr[?, LocalDate] = Expr.Now

    val pass: Expr[?, Boolean] = literal(true)

    @nowarn("cat=unused")
    implicit def literal[A, B: FieldType](value: B): Expr[A, B] =
      Literal(value)

    implicit def seq[B: FieldType](bs: List[B]): List[Expr[Any, B]] =
      bs.map(literal(_))

    @nowarn("cat=unused")
    implicit def recordToExpr[B: FieldType](jsonRecord: JsonRecord[B]): Field[Json, B] =
      Expr.Field(jsonRecord.path)(jsonRecord.fieldType)

    @nowarn("cat=unused")
    def equalTo[A, B: FieldType: Eq](expr: Expr[A, B]): Expr[A, B] => Expr[A, Boolean] =
      _ === expr

    def hasLength[A](max: Int): Expr[A, String] => Expr[A, Boolean] =
      _.length === max
  }
}
