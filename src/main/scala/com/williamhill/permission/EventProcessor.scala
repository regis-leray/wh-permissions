package com.williamhill.permission

import com.williamhill.permission.application.AppError
import com.williamhill.permission.kafka.events.generic.InputEvent
import com.williamhill.platform.event.permission.Event as OutputEvent
import zio.clock.Clock
import zio.{Has, IO, URLayer, ZIO}

trait EventProcessor {
  def handleInput(topic: String, inputEvent: InputEvent): IO[AppError, OutputEvent]
}

object EventProcessor {

  def handleInput(topic: String, inputEvent: InputEvent): ZIO[Has[EventProcessor], AppError, OutputEvent] =
    ZIO.serviceWith[EventProcessor](_.handleInput(topic, inputEvent))

  class EventProcessorImpl(
      facetContextParser: FacetContextParser,
      permissionLogic: PermissionLogic,
      clock: Clock.Service,
  ) extends EventProcessor {
    override def handleInput(topic: String, inputEvent: InputEvent): IO[AppError, OutputEvent] =
      for {
        facetContext <- ZIO.fromEither(facetContextParser.parse(topic, inputEvent))
        now          <- clock.instant
        isExpired = facetContext.newStatus.endDate.exists(_.isBefore(now))
        _                       <- ZIO.when(isExpired)(ZIO.fail(AppError.fromMessage(s"Facet context has expired")))
        facetContextWithActions <- permissionLogic.enrichWithActions(facetContext)
      } yield OutputEvent(facetContextWithActions)
  }

  val layer: URLayer[Has[PermissionLogic] & Has[FacetContextParser] & Clock, Has[EventProcessor]] = (for {
    facetContextParser <- ZIO.service[FacetContextParser]
    permissionLogic    <- ZIO.service[PermissionLogic]
    clock              <- ZIO.service[Clock.Service]
  } yield new EventProcessorImpl(facetContextParser, permissionLogic, clock)).toLayer

}
