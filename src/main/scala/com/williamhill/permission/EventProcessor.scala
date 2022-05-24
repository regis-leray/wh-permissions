package com.williamhill.permission

import com.williamhill.permission.application.AppError
import com.williamhill.permission.kafka.events.generic.{InputEvent, OutputEvent}
import zio.{Has, IO, URLayer, ZIO}

trait EventProcessor {
  def handleInput(topic: String, inputEvent: InputEvent): IO[AppError, OutputEvent]
}

object EventProcessor {

  def handleInput(topic: String, inputEvent: InputEvent): ZIO[Has[EventProcessor], AppError, OutputEvent] =
    ZIO.serviceWith[EventProcessor](_.handleInput(topic, inputEvent))

  class EventProcessorImpl(facetContextParser: FacetContextParser, permissionLogic: PermissionLogic) extends EventProcessor {
    override def handleInput(topic: String, inputEvent: InputEvent): IO[AppError, OutputEvent] =
      for {
        facetContext            <- ZIO.fromEither(facetContextParser.parse(topic, inputEvent))
        facetContextWithActions <- permissionLogic.enrichWithActions(facetContext).mapError(AppError.fromThrowable)
      } yield OutputEvent(facetContextWithActions)
  }

  val layer: URLayer[Has[PermissionLogic] & Has[FacetContextParser], Has[EventProcessor]] = (for {
    facetContextParser <- ZIO.service[FacetContextParser]
    permissionLogic    <- ZIO.service[PermissionLogic]
  } yield new EventProcessorImpl(facetContextParser, permissionLogic)).toLayer

}
