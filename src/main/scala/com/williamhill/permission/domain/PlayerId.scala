package com.williamhill.permission.domain

import scala.util.matching.Regex
import com.williamhill.permission.application.AppError
import io.circe.Decoder

final case class PlayerId private (value: String) {
  override def toString: String = value
}

object PlayerId {
  private val allowedPrefix = "[A-Z0-9]"
  // TODO: is this regex correct?
  // private val unityIdRegex: Regex = s"($allowedPrefix[A-HJKMNP-TV-Z0-9]{9})".r
  private val unityIdRegex: Regex = s"($allowedPrefix{9})".r
  def apply(value: String): Either[AppError, PlayerId] =
    Either.cond(
      unityIdRegex.matches(value),
      new PlayerId(value),
      AppError(s"Id value: '$value' does not match unity id regular expression pattern: $unityIdRegex"),
    )

  implicit val decoder: Decoder[PlayerId] = Decoder.decodeString.emap(s => apply(s).left.map(_.message))
}
