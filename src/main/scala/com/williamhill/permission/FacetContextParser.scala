package com.williamhill.permission

import cats.syntax.traverse.*
import com.williamhill.permission.application.AppError
import com.williamhill.permission.application.config.{Mapping, MappingsConfig}
import com.williamhill.permission.domain.{FacetContext, PermissionStatus, Universe}
import com.williamhill.permission.kafka.events.generic.InputEvent
import com.williamhill.permission.utils.JsonSyntax
import io.circe.ACursor
import zio.{Has, URLayer, ZIO}

class FacetContextParser(config: MappingsConfig) extends JsonSyntax {

  def parse(input: InputEvent): Either[AppError, FacetContext] = {
    val body = input.body.hcursor

    for {
      eventType <- body.downField("type").as[String].left.map(AppError.fromDecodingFailure)
      mapping   <- config.mappings.find(_.eventType.equalsIgnoreCase(eventType)).toRight(AppError.missingMapping(eventType))

      newValues      = body.downField("newValues")
      previousValues = body.downField("previousValues")

      universe <- Universe(input.header.universe)
      playerId <- newValues.evaluate(mapping.playerId.value).left.map(AppError.fromDecodingFailure)

      newStatuses <- parsePermissionStatuses(mapping)(newValues)
      oldStatuses <- previousValues.focus.toList.map(_.hcursor).flatTraverse(parsePermissionStatuses(mapping))

    } yield FacetContext(
      header = input.header,
      actions = Nil,
      playerId = playerId,
      universe = universe,
      name = eventType,
      newStatuses = newStatuses,
      previousStatuses = oldStatuses,
    )
  }

  private def parsePermissionStatuses(m: Mapping)(cursor: ACursor): Either[AppError, List[PermissionStatus]] = {
    (for {
      statuses <- cursor.evaluateList(m.status)
      start    <- m.actionsStart.flatTraverse(cursor.evaluateFirst(_))
      end      <- m.actionsEnd.flatTraverse(cursor.evaluateFirst(_))
    } yield statuses.map(PermissionStatus(_, start, end))).left.map(AppError.fromDecodingFailure)
  }

}

object FacetContextParser {

  val layer: URLayer[Has[MappingsConfig], Has[FacetContextParser]] =
    ZIO.service[MappingsConfig].map(new FacetContextParser(_)).toLayer

}
