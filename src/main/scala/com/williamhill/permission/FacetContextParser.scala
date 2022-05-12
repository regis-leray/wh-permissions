package com.williamhill.permission

import cats.syntax.traverse.*
import com.williamhill.permission.application.AppError
import com.williamhill.permission.application.config.{Mapping, MappingsConfig}
import com.williamhill.permission.domain.{FacetContext, PermissionStatus, PlayerId, Universe}
import com.williamhill.permission.kafka.events.generic.InputEvent
import io.circe.{ACursor, Decoder, DecodingFailure}
import zio.{Has, RLayer, ZIO}

import java.time.Instant

class FacetContextParser(config: MappingsConfig) {

  def parse(input: InputEvent): Either[AppError, FacetContext] = {
    val body = input.body.hcursor

    for {
      eventType <- body.downField("type").as[String].left.map(AppError.fromDecodingFailure)
      mapping   <- config.mappings.find(_.eventType.equalsIgnoreCase(eventType)).toRight(AppError.missingMapping(eventType))

      newValues      = body.downField("newValues")
      previousValues = body.downField("previousValues")

      universe <- Universe(input.header.universe)
      playerId <- newValues.downPath(mapping.playerId).as[PlayerId].left.map(AppError.fromDecodingFailure)

      newStatus <- parsePermissionStatus(mapping)(newValues)
      oldStatus <- previousValues.focus.map(_.hcursor).traverse(parsePermissionStatus(mapping))

    } yield FacetContext(
      header = input.header,
      actions = Nil,
      playerId = playerId,
      universe = universe,
      name = eventType,
      newStatus = newStatus,
      previousStatus = oldStatus,
    )
  }

  implicit private class CursorExt(cursor: ACursor) {
    def downPath(path: String): ACursor =
      downPath(path.split('.').toList)

    def downPath(path: List[String]): ACursor =
      path.foldLeft(cursor)(_ downField _)

    def findFirst[T](options: List[String])(implicit decoder: Decoder[Option[T]]): Either[DecodingFailure, Option[T]] = options match {
      case Nil => Right(None)
      case head :: tail =>
        cursor.downPath(head).as[Option[T]].flatMap {
          case None        => findFirst(tail)
          case Some(found) => Right(Some(found))
        }
    }
  }

  private def parsePermissionStatus(m: Mapping)(cursor: ACursor): Either[AppError, PermissionStatus] = {
    (for {
      status <- cursor.downPath(m.status).as[String]
      start  <- m.actionsStart.flatTraverse(cursor.findFirst[Instant])
      end    <- m.actionsEnd.flatTraverse(cursor.findFirst[Instant])
    } yield PermissionStatus(status, start, end)).left.map(AppError.fromDecodingFailure)
  }

}

object FacetContextParser {

  val layer: RLayer[Has[MappingsConfig], Has[FacetContextParser]] =
    ZIO.service[MappingsConfig].map(new FacetContextParser(_)).toLayer

}
