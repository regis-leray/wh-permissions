package com.williamhill.permission.domain

import java.time.Instant

import cats.syntax.either.*
import io.circe.*
import io.circe.generic.semiauto.deriveCodec

import com.williamhill.permission.application.AppError

final case class PermissionStatus(
    status: String,
    startDate: Option[Instant] = None,
    endDate: Option[Instant] = None,
)

object PermissionStatus {
  implicit val codec: Codec[PermissionStatus] = deriveCodec
  // TODO: should this logic be moved into InputParser?
  def fromJsonBodyValues(eventType: String)(values: Json): Either[AppError, PermissionStatus] =
    eventType match {
      case "excluded" =>
        for {
          status <- values.hcursor.downField("exclusion").downField("type").as[String].leftMap(AppError.fromDecodingFailure)
          permissionContext <- status match {
            case "permanent" | "indefinite" =>
              (for {
                maybeStart    <- values.hcursor.downField("exclusion").downField("start").as[Option[Instant]]
                realizedStart <- values.hcursor.downField("exclusion").downField("realizedStart").as[Instant]
                start = maybeStart.getOrElse(realizedStart)
              } yield PermissionStatus(status, Some(start))).leftMap(AppError.fromDecodingFailure)
            case "temporary" | "timeout" =>
              (for {
                maybeStart    <- values.hcursor.downField("exclusion").downField("start").as[Option[Instant]]
                realizedStart <- values.hcursor.downField("exclusion").downField("realizedStart").as[Instant]
                maybeEnd      <- values.hcursor.downField("exclusion").downField("end").as[Option[Instant]]
                maybeRealizedEnd <- values.hcursor
                  .downField("exclusion")
                  .downField("realizedEnd")
                  .as[Option[Instant]] // TODO check why realizedEnd is optional
                start = maybeStart.getOrElse(realizedStart)
                end   = maybeEnd.orElse(maybeRealizedEnd)
              } yield PermissionStatus(status, Some(start), end)).leftMap(AppError.fromDecodingFailure)
            case _ => Left(AppError(s"Invalid exclusion status: $status"))
          }
        } yield permissionContext
      case "Dormancy" | "Prohibition" =>
        values.hcursor
          .downField("data")
          .downField("status")
          .as[String]
          .leftMap(AppError.fromDecodingFailure)
          .map(s => PermissionStatus(s))
      case et => Left(AppError.fromMessage(s"Unrecognised input event type $et"))
    }

}
