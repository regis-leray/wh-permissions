package com.williamhill.permission.domain

import scala.util.matching.Regex

import com.williamhill.permission.application.AppError
import io.circe.Decoder
import pureconfig.ConfigReader
import pureconfig.error.CannotConvert

final case class PlayerId private (value: String) {
  override def toString: String = value
}

object PlayerId {
  private val alnum     = "[a-zA-Z0-9]"
  private val uuidRegex = s"$alnum{8}-$alnum{4}-$alnum{4}-$alnum{4}-$alnum{12}"
  // TODO: is this regex correct?
  // private val unityIdRegex: Regex = s"($allowedPrefix[A-HJKMNP-TV-Z0-9]{9})".r
  private val unityIdRegex: Regex = s"($alnum{9}|$uuidRegex)".r
  def apply(value: String): Either[AppError, PlayerId] =
    Either.cond(
      unityIdRegex.matches(value),
      new PlayerId(value),
      AppError(s"Id value: '$value' does not match unity id regular expression pattern: $unityIdRegex"),
    )

  implicit val decoder: Decoder[PlayerId] = Decoder.decodeString.emap(s => apply(s).left.map(_.message))

  implicit val reader: ConfigReader[PlayerId] =
    ConfigReader.stringConfigReader
      .emap(s => apply(s).left.map(ex => CannotConvert(s, "PlayerId", ex.message)))
}
