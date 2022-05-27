package com.williamhill.permission.dsl

import scala.util.matching.Regex
import scala.util.matching.Regex.Match

import com.williamhill.permission.dsl.SeqSyntax.SeqExt
import pureconfig.ConfigReader
import pureconfig.error.CannotConvert

sealed trait Value[+T] {
  def optional: Value[Option[T]]
}

object Value {
  type Reader[A] = ConfigReader[Value[A]]

  case class Const[+T](value: T) extends Value[T] {
    def optional: Const[Option[T]] = Const(Some(value))
  }

  object Const {
    implicit def reader[T: ConfigReader]: ConfigReader[Const[T]] = ConfigReader[T].map(Const(_))
  }

  sealed trait JsonPath extends Value[Nothing] {
    def optional: Value[Nothing]          = this
    def /:(s: JsonPath.Segment): JsonPath = JsonPath.NonEmpty(s, this)
  }

  object JsonPath {

    case object Empty extends JsonPath

    case class NonEmpty(head: Segment, tail: JsonPath) extends JsonPath

    def apply(segments: Seq[Segment]): JsonPath = segments.foldRight[JsonPath](Empty)(_ /: _)

    sealed trait Segment {
      def toPath: JsonPath = NonEmpty(this, Empty)
    }

    case class Property(key: String)          extends Segment
    case class ArrayElement(index: Int)       extends Segment
    case class ArrayRange(from: Int, to: Int) extends Segment
    case object Wildcard                      extends Segment

    private val propertyRegexDot      = "\\.([a-zA-Z_]+)".r
    private val propertyRegexBrackets = "\\['([a-zA-Z_]+)']".r
    private val arrayElementRegex     = "\\[(\\d+)]".r
    private val arrayRangeRegex       = "\\[(\\d+):(\\d+)]".r
    private val wildcardRegex         = "\\.\\*|\\[\\*]".r

    private def propertyMatch(m: Match): Property         = Property(m.group(1))
    private def arrayElementMatch(m: Match): ArrayElement = ArrayElement(m.group(1).toInt)
    private def arrayRangeMatch(m: Match): ArrayRange     = ArrayRange(m.group(1).toInt, m.group(2).toInt)

    private val segments: List[(Regex, Match => Segment)] = List(
      propertyRegexDot      -> propertyMatch,
      propertyRegexBrackets -> propertyMatch,
      arrayElementRegex     -> arrayElementMatch,
      arrayRangeRegex       -> arrayRangeMatch,
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

  implicit def reader[T: ConfigReader]: ConfigReader[Value[T]] = JsonPath.reader.orElse(Const.reader[T])
}
