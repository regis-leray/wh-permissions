package com.williamhill.permission

import cats.syntax.either.*
import cats.syntax.option.*
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

  private val parseEventType: Parser[String] =
    _.record.value().body.hcursor.downField("type").as[String].leftMap(AppError.fromDecodingFailure)

  private val parsePlayerId: String => Parser[PlayerId] = { eventType => record =>
    val jsonBody = record.record.value().body
    (eventType match {
      case "excluded" =>
        jsonBody.hcursor.downField("newValues").downField("playerId").as[String].leftMap(AppError.fromDecodingFailure)
      case "Dormancy" | "Prohibition" =>
        jsonBody.hcursor.downField("newValues").downField("id").as[String].leftMap(AppError.fromDecodingFailure)
      case et =>
        Left(AppError.fromMessage(s"Unrecognised input event type $et"))
    }).flatMap(PlayerId.apply)
  }

  private val newPermissionStatus: String => Parser[PermissionStatus] = eventType =>
    event =>
      event.record
        .value()
        .body
        .hcursor
        .downField("newValues")
        .as[Json]
        .leftMap(AppError.fromDecodingFailure)
        .flatMap(PermissionStatus.fromJsonBodyValues(eventType))

  private val previousPermissionStatus: String => Parser[Option[PermissionStatus]] = eventType =>
    event =>
      event.record
        .value()
        .body
        .hcursor
        .downField("previousValues")
        .as[Option[Json]]
        .leftMap(AppError.fromDecodingFailure)
        .flatMap {
          case None         => none.asRight
          case Some(values) => PermissionStatus.fromJsonBodyValues(eventType)(values).map(_.some)
        }

  private val parseFacetContext: Parser[FacetContext] =
    event =>
      for {
        eventType      <- parseEventType(event)
        playerId       <- parsePlayerId(eventType)(event)
        universe       <- parseUniverse(event)
        newStatus      <- newPermissionStatus(eventType)(event)
        previousStatus <- previousPermissionStatus(eventType)(event)
      } yield FacetContext(
        header = event.value.header,
        actions = Nil,
        playerId = playerId,
        universe = universe,
        name = eventType,
        newStatus = newStatus,
        previousStatus = previousStatus,
      )

  def parse(event: InputRecord): Option[FacetContext] =
    parseFacetContext(event) match {
      case Left(error) =>
        logger.error(error, s"Failed to parse event: $event")
        None
      case Right(result) => Some(result)
    }

}
