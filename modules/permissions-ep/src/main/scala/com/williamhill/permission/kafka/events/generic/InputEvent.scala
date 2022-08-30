package com.williamhill.permission.kafka.events.generic

import cats.Eq
import cats.syntax.either.*
import com.williamhill.permission.application.AppError
import io.circe.*
import io.circe.generic.semiauto.deriveCodec
import io.github.azhur.kafkaserdecirce.CirceSupport
import org.apache.commons.lang3.StringUtils
import org.apache.kafka.common.serialization.Serde as KafkaSerde

final case class InputEvent(header: InputHeader, body: Json)

object InputEvent {
  implicit val codec: Codec[InputEvent]       = deriveCodec
  implicit val kserde: KafkaSerde[InputEvent] = CirceSupport.toSerde

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
      value <- codec.decodeJson(json).leftMap(AppError.fromDecodingFailure)
    } yield value
  }
}

object OutputEvent {
  type OutputEvent = com.williamhill.platform.event.permission.Event
  implicit val codec: Codec[OutputEvent]       = OutputEvent.codec
  implicit val kserde: KafkaSerde[OutputEvent] = CirceSupport.toSerde
}
