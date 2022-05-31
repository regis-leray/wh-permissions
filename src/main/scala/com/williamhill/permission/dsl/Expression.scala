package com.williamhill.permission.dsl

import scala.util.matching.Regex
import scala.util.matching.Regex.Match

import cats.implicits.toTraverseOps
import com.williamhill.permission.dsl.SeqSyntax.SeqExt
import io.circe.{Encoder, Json}
import pureconfig.ConfigReader
import pureconfig.error.{CannotConvert, ConfigReaderFailures, ConvertFailure, KeyNotFound}

sealed trait Expression

object Expression {

  case class Const(value: Json) extends Expression

  object Const {
    def apply[T: Encoder](value: T): Const   = Const(Encoder[T].apply(value))
    implicit val reader: ConfigReader[Const] = JsonConfigReader.jsonReader.map(Const(_))
  }

  sealed trait JsonPath extends Expression {
    def /:(s: JsonPath.Segment): JsonPath = JsonPath.NonEmpty(s, this)
  }

  object JsonPath {

    case object Empty extends JsonPath

    case class NonEmpty(head: Segment, tail: JsonPath) extends JsonPath

    def apply(segments: Seq[Segment]): JsonPath = segments.foldRight[JsonPath](Empty)(_ /: _)

    sealed trait Segment {
      def toPath: JsonPath = NonEmpty(this, Empty)
    }

    case class Property(key: String)                          extends Segment
    case class ArrayElement(index: Int)                       extends Segment
    case class ArrayRange(from: Option[Int], to: Option[Int]) extends Segment
    case object Wildcard                                      extends Segment

    private val propertyRegexDot      = "\\.([a-zA-Z\\d_-]+)".r
    private val propertyRegexBrackets = "\\['([a-zA-Z\\d_-]+)']".r
    private val arrayElementRegex     = "\\[(-?\\d+)]".r
    private val arrayRangeRegex       = "\\[(-?\\d+):(-?\\d+)]".r
    private val arrayRangeFromRegex   = "\\[(-?\\d+):]".r
    private val arrayRangeToRegex     = "\\[:(-?\\d+)]".r
    private val wildcardRegex         = "\\.\\*|\\[\\*]".r

    private def propertyMatch(m: Match): Property         = Property(m.group(1))
    private def arrayElementMatch(m: Match): ArrayElement = ArrayElement(m.group(1).toInt)
    private def arrayRangeMatch(m: Match): ArrayRange     = ArrayRange(Some(m.group(1).toInt), Some(m.group(2).toInt))
    private def arrayRangeFromMatch(m: Match): ArrayRange = ArrayRange(Some(m.group(1).toInt), None)
    private def arrayRangeToMatch(m: Match): ArrayRange   = ArrayRange(None, Some(m.group(1).toInt))

    private val segments: List[(Regex, Match => Segment)] = List(
      propertyRegexDot      -> propertyMatch,
      propertyRegexBrackets -> propertyMatch,
      arrayElementRegex     -> arrayElementMatch,
      arrayRangeRegex       -> arrayRangeMatch,
      arrayRangeFromRegex   -> arrayRangeFromMatch,
      arrayRangeToRegex     -> arrayRangeToMatch,
      wildcardRegex         -> (_ => Wildcard),
    )

    private def parseR(s: String): Either[String, List[Segment]] = {
      if (s.isEmpty) Right(Nil)
      else {
        segments
          .collectSome { case (regex, parser) => regex.findPrefixMatchOf(s).map(m => parser(m) -> m.source.toString.drop(m.end)) }
          .toRight(s"Invalid JSON path: $s")
          .flatMap { case (head, unmatched) => parseR(unmatched).map(head :: _) }
      }
    }

    def parse(s: String): Either[String, JsonPath] =
      if (s.startsWith("$")) parseR(s.drop(1)).map(JsonPath.apply)
      else Left(s"Invalid JSON path: $s")

    implicit val reader: ConfigReader[JsonPath] =
      ConfigReader.stringConfigReader.emap(s => parse(s).left.map(reason => CannotConvert(s, "JsonPath", reason)))
  }

  case class Conditional(
      value: Expression,
      when: Option[BooleanExpression] = None,
      defaultTo: Option[Expression] = None,
  ) extends Expression

  object Conditional {
    implicit val reader: ConfigReader[Conditional] = {
      ConfigReader.fromCursor { cursor =>
        for {
          obj <- cursor.asMap
          value <- obj.get("value") match {
            case Some(valueCursor) => Expression.reader.from(valueCursor)
            case None              => Left(ConfigReaderFailures(ConvertFailure(KeyNotFound("value", obj.keys.toSet), cursor)))
          }
          when    <- obj.get("when").traverse(BooleanExpression.reader.from)
          default <- obj.get("default-to").traverse(Expression.reader.from)
        } yield Conditional(value, when, default)
      }
    }
  }

  case class Expressions(expressions: Vector[Expression]) extends Expression

  object Expressions {
    implicit val reader: ConfigReader[Expressions] =
      ConfigReader[Vector[Expression]].map(Expressions(_))
  }

  implicit val reader: ConfigReader[Expression] = {
    JsonPath.reader
      .orElse(Const.reader)
      .orElse(Conditional.reader)
      .orElse(Expressions.reader)
  }

}
