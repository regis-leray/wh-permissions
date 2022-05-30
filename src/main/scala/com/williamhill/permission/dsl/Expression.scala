package com.williamhill.permission.dsl

import scala.util.matching.Regex
import scala.util.matching.Regex.Match

import cats.syntax.traverse.*
import com.williamhill.permission.dsl.SeqSyntax.SeqExt
import pureconfig.ConfigReader
import pureconfig.error.{CannotConvert, ConfigReaderFailures, ConvertFailure, KeyNotFound}

sealed trait Expression[+T]

object Expression {

  type Reader[T] = ConfigReader[Expression[T]]

  case class Const[+T](value: T) extends Expression[T]

  object Const {
    implicit def reader[T: ConfigReader]: ConfigReader[Const[T]] = ConfigReader[T].map(Const(_))
  }

  sealed trait JsonPath extends Expression[Nothing] {
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

  case class Conditional[+T](
      value: Expression[T],
      when: Option[BooleanExpression] = None,
      defaultTo: Option[Expression[T]] = None,
  ) extends Expression[T]

  object Conditional {
    implicit def reader[T: ConfigReader]: ConfigReader[Conditional[T]] = {
      ConfigReader.fromCursor { cursor =>
        for {
          obj <- cursor.asMap
          value <- obj.get("value") match {
            case Some(valueCursor) => Expression.reader[T].from(valueCursor)
            case None              => Left(ConfigReaderFailures(ConvertFailure(KeyNotFound("value", obj.keys.toSet), cursor)))
          }
          when    <- obj.get("when").traverse(BooleanExpression.reader.from)
          default <- obj.get("default-to").traverse(Expression.reader[T].from)
        } yield Conditional(value, when, default)
      }
    }
  }

  case class Expressions[+T](expressions: Vector[Expression[T]]) extends Expression[T]

  object Expressions {
    implicit def reader[T: ConfigReader]: ConfigReader[Expressions[T]] =
      ConfigReader[Vector[Expression[T]]].map(Expressions(_))
  }

  implicit def reader[T: ConfigReader]: ConfigReader[Expression[T]] = {
    JsonPath.reader
      .orElse(Const.reader[T])
      .orElse(Conditional.reader[T])
      .orElse(Expressions.reader[T])
  }

}
