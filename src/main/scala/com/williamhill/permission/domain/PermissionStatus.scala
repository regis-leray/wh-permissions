package com.williamhill.permission.domain

import java.time.Instant

import io.circe.Json
import cats.syntax.either.*

import com.williamhill.permission.application.AppError
//sealed trait PermissionStatus

//TODO remove enum

final case class PermissionStatus(startDate: Instant, maybeEndDate: Option[Instant], status: String)

object PermissionStatus {

  def mk(eventType: String, json: Json): Either[AppError, PermissionStatus] =
    eventType match {
      case "excluded" =>
        for {
          status <- json.hcursor
            .downField("newValues")
            .downField("exclusion")
            .downField("type")
            .as[String]
            .leftMap(AppError.fromDecodingFailure)
          result <- status match {

            case "permanent" | "indefinite" =>
              (for {
                maybeStart    <- json.hcursor.downField("start").as[Option[Instant]]
                realizedStart <- json.hcursor.downField("realizedStart").as[Instant]
                start = maybeStart.getOrElse(realizedStart)
              } yield PermissionStatus(start, maybeEndDate = None, status = status)).leftMap(AppError.fromDecodingFailure)

            case "temporary" | "timeout" =>
              (for {
                maybeStart       <- json.hcursor.downField("start").as[Option[Instant]]
                realizedStart    <- json.hcursor.downField("realizedStart").as[Instant]
                maybeEnd         <- json.hcursor.downField("end").as[Option[Instant]]
                maybeRealizedEnd <- json.hcursor.downField("realizedEnd").as[Option[Instant]] //TODO check why realizedEnd is optional
                start = maybeStart.getOrElse(realizedStart)
                end   = maybeEnd.orElse(maybeRealizedEnd)
              } yield PermissionStatus(start, end, status = status)).leftMap(AppError.fromDecodingFailure)

            case _ => Left(AppError(s"Invalid exclusion status: $status"))
          }
        } yield result

      case _ => Left(AppError(s"Unknown event type: $eventType"))
    }

}
