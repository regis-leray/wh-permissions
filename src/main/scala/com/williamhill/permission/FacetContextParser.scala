package com.williamhill.permission

import java.time.Instant

import cats.syntax.either.*
import cats.syntax.option.*
import cats.syntax.traverse.*
import com.williamhill.permission.application.AppError
import com.williamhill.permission.application.config.{Mapping, MappingsConfig}
import com.williamhill.permission.domain.{FacetContext, PermissionStatus, PlayerId, Universe}
import com.williamhill.permission.dsl.ExpressionEvaluator
import com.williamhill.permission.dsl.SeqSyntax.SeqExt
import com.williamhill.permission.kafka.events.generic.InputEvent
import zio.{Has, URLayer, ZIO}

class FacetContextParser(config: MappingsConfig) {

  def parse(topic: String, input: InputEvent): Either[AppError, FacetContext] = {
    val evaluator = new ExpressionEvaluator(input.body.hcursor)

    for {
      eventTypeAndMapping <-
        config.mappings
          .filter(_.topics.forall(_.contains(topic)))
          .collectSome(mapping => evaluator.evaluate[String](mapping.event.toExpression).toOption.map(_ -> mapping))
          .toRight(AppError.eventTypeNotFound(input.body))

      (event, mapping) = eventTypeAndMapping

      newValues      = evaluator.mapCursor(_.downField("newValues"))
      previousValues = evaluator.mapCursor(_.downField("previousValues"))

      universe <- Universe(input.header.universe)
      playerId <- newValues.evaluate[PlayerId](mapping.playerId).left.map(AppError.fromDecodingFailure)

      newStatuses <- parsePermissionStatuses(mapping)(newValues)
      oldStatuses <- previousValues.optional.fold(none[PermissionStatus].asRight[AppError])(
        parsePermissionStatuses(mapping)(_).map(Some(_)),
      )

    } yield FacetContext(
      header = input.header,
      actions = Vector.empty,
      playerId = playerId,
      universe = universe,
      name = event,
      newStatus = newStatuses,
      previousStatus = oldStatuses,
    )
  }

  private def parsePermissionStatuses(m: Mapping)(evaluator: ExpressionEvaluator): Either[AppError, PermissionStatus] = {
    (for {
      statuses <- evaluator.evaluateVector[String](m.status)
      start    <- m.actionsStart.flatTraverse(evaluator.evaluate[Option[Instant]])
      end      <- m.actionsEnd.flatTraverse(evaluator.evaluate[Option[Instant]])
    } yield PermissionStatus(statuses, start, end)).left.map(AppError.fromDecodingFailure)
  }

}

object FacetContextParser {

  val layer: URLayer[Has[MappingsConfig], Has[FacetContextParser]] =
    ZIO.service[MappingsConfig].map(new FacetContextParser(_)).toLayer

}
