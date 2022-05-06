package com.williamhill.permission

import cats.syntax.either.*

import com.typesafe.scalalogging.LazyLogging
import com.williamhill.permission.application.AppError
import com.williamhill.permission.application.logging.*
import com.williamhill.permission.domain.{FacetContext, PermissionStatus, PlayerId, Universe}
import com.williamhill.permission.kafka.Record.InputRecord
import io.circe.Json

object InputParser extends LazyLogging {
  type Parser[A] = InputRecord => Either[AppError, A]

  val parseUniverse: Parser[Universe] =
    record => Universe(record.value.header.universe)

  val parseEventType: Parser[String] = event => {

    val json = event.record.value().body

    json.hcursor.downField("type").as[String].leftMap(AppError.fromDecodingFailure)
  }

  private val parsePlayerId: Parser[PlayerId] =
    record => PlayerId(record.record.value().header.playerId)

  private val permissionStatus: Parser[PermissionStatus] = event => {

    //TODO check if we can merge this function with `PermissionStatus.mk`
    val json = event.record.value().body

    parseEventType(event).toOption match {
      case Some("excluded") =>
        for {

          newValues <- json.hcursor.downField("newValues").as[Json].leftMap(AppError.fromDecodingFailure)
          result    <- PermissionStatus.mk("excluded", newValues)
        } yield result

      case _ =>
        Left(AppError.fromMessage("Type is not excluded"))
    }
  }

  private val parseFacetContext: String => Parser[FacetContext] = name =>
    event =>
      for {
        status   <- permissionStatus(event)
        playerId <- parsePlayerId(event)
        universe <- parseUniverse(event)
      } yield FacetContext(
        header = event.value.header,
        actions = Nil,
        playerId = playerId,
        universe = universe,
        name = name,
        status = status,
      )

  def parse(name: String, event: InputRecord): Option[FacetContext] = {
    val resultEither = parseFacetContext(name)(event)

    resultEither match {
      case Left(error) =>
        logger.error(error, s"Failed to parse event: $event")
        None
      case Right(result) => Some(result)
    }
  }

}
