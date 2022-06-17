package com.williamhill.permission.kafka.events.generic

import cats.Eq
import cats.syntax.either.*
import com.williamhill.permission.application.AppError
import io.circe.*
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import org.apache.commons.lang3.StringUtils

final case class InputEvent(header: InputHeader, body: Json)

object InputEvent {

  implicit val encoder: Encoder[InputEvent] = deriveEncoder

//  private val decoderStrict: Decoder[InputEvent] = deriveDecoder
//  private val decoderClean: Decoder[InputEvent] =
//    Decoder.decodeString.emap { s =>
//      val startIndex = StringUtils.indexOf(s, "{")
//      val endIndex   = StringUtils.lastIndexOf(s, "}") + 1
//      val clean      = StringUtils.substring(s, startIndex, endIndex)
//      for {
//        json  <- parser.parse(s).leftMap(pf => pf.message)
//        value <- decoderStrict.decodeJson(json).leftMap(df => df.message)
//      } yield value
//
//    }

  implicit val decoder: Decoder[InputEvent] = deriveDecoder

  implicit val eq: Eq[InputEvent] = Eq.fromUniversalEquals[InputEvent]

  def clean(event: String): String = {

    val startIndex = StringUtils.indexOf(event, "{")
    val endIndex   = StringUtils.lastIndexOf(event, "}") + 1
    StringUtils.substring(event, startIndex, endIndex)
  }

  def produce(source: String): Either[AppError, InputEvent] = {

    val cleanSource = clean(source)
    for {
      json  <- parser.parse(cleanSource).leftMap(pf => AppError.fromMessage(pf.message))
      value <- decoder.decodeJson(json).leftMap(AppError.fromDecodingFailure)
    } yield value
  }
}
