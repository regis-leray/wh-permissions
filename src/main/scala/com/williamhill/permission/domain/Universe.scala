package com.williamhill.permission.domain

import scala.util.matching.Regex

import com.williamhill.permission.application.AppError
import io.circe.Decoder

final case class Universe private (value: String) {
  override def toString: String = value
}

object Universe {
  private val pattern: Regex = """^[a-zA-Z]+-[a-zA-Z]+(-[a-zA-Z]+)*$""".r

  def apply(value: String): Either[AppError, Universe] = pattern.findFirstIn(value) match {
    case Some(value) => Right(new Universe(value))
    case _           => Left(AppError(s"Universe must follow this pattern: $pattern"))
  }

  implicit val decoder: Decoder[Universe] = Decoder.decodeString.emap(s => apply(s).left.map(_.message))

}
