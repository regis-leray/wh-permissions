package com.williamhill.permission

import scala.annotation.tailrec

import cats.syntax.traverse.*
import com.williamhill.permission.application.AppError
import com.williamhill.permission.application.config.{Mapping, MappingsConfig}
import com.williamhill.permission.domain.{FacetContext, PermissionStatus, Universe}
import com.williamhill.permission.kafka.events.generic.InputEvent
import com.williamhill.permission.utils.JsonSyntax
import io.circe.ACursor
import zio.{Has, URLayer, ZIO}

class FacetContextParser(config: MappingsConfig) extends JsonSyntax {

  def parse(topic: String, input: InputEvent): Either[AppError, FacetContext] = {
    val body = input.body.hcursor

    for {
      eventTypeAndMapping <-
        config.mappings
          .filter(_.topics.forall(_.contains(topic)))
          .collectSome(mapping => body.evaluateRequired(mapping.eventType.toExpression).toOption.map(_ -> mapping))
          .toRight(AppError.eventTypeNotFound(input.body))

      (eventType, mapping) = eventTypeAndMapping

      newValues      = body.downField("newValues")
      previousValues = body.downField("previousValues")

      universe <- Universe(input.header.universe)
      playerId <- newValues.evaluateRequired(mapping.playerId).left.map(AppError.fromDecodingFailure)

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
      start    <- m.actionsStart.flatTraverse(cursor.evaluateOption(_))
      end      <- m.actionsEnd.flatTraverse(cursor.evaluateOption(_))
    } yield statuses.map(PermissionStatus(_, start, end))).left.map(AppError.fromDecodingFailure)
  }

  implicit private class ListExt[T](list: List[T]) {
    final def collectSome[U](f: T => Option[U]): Option[U] = collectSomeRec(list, f)

    @tailrec
    final private def collectSomeRec[U](list: List[T], f: T => Option[U]): Option[U] = list match {
      case Nil => None
      case head :: tail =>
        f(head) match {
          case some @ Some(_) => some
          case None           => collectSomeRec(tail, f)
        }
    }
  }

}

object FacetContextParser {

  val layer: URLayer[Has[MappingsConfig], Has[FacetContextParser]] =
    ZIO.service[MappingsConfig].map(new FacetContextParser(_)).toLayer

}
