package com.williamhill.permission

import java.time.Instant

import cats.syntax.traverse.*
import com.williamhill.permission.application.config.RulesConfig
import com.williamhill.permission.domain.{Action, FacetContext}
import com.williamhill.permission.dsl.ExpressionEvaluator
import io.circe.{DecodingFailure, Json, JsonObject}
import zio.clock.Clock
import zio.{Has, Task, URLayer, ZIO}

trait PermissionLogic {
  def enrichWithActions(facetContext: FacetContext): Task[FacetContext]
}

class PermissionLogicLive(config: RulesConfig, clock: Clock.Service) extends PermissionLogic {

  override def enrichWithActions(fc: FacetContext): Task[FacetContext] =
    for {
      now    <- clock.instant
      result <- Task.fromEither(enrichWithActions(fc, now))
    } yield result

  def enrichWithActions(fc: FacetContext, now: Instant): Either[DecodingFailure, FacetContext] = {
    fc.newStatuses
      .flatTraverse { status =>
        val evaluator = new ExpressionEvaluator(
          Json
            .fromJsonObject(
              JsonObject(
                "universe" -> Json.fromString(fc.universe.value),
                "event"    -> Json.fromString(fc.name),
                "status"   -> Json.fromString(status.status),
              ),
            )
            .hcursor,
        )

        config.rules
          .flatTraverse(evaluator.evaluateAll[String])
          .map(actionNames => config.actions.filter(ad => actionNames.contains(ad.name)))
          .map(actionDefinitions =>
            actionDefinitions
              .map(ad =>
                Action(
                  ad.`type`,
                  ad.name,
                  ad.reasonCode,
                  ad.denialDescription,
                  ad.deniedPermissions,
                  status.endDate.filter(_.isAfter(now)),
                ),
              ),
          )
      }
      .map(actions => fc.copy(actions = actions))
  }

}

object PermissionLogic {
  val layer: URLayer[Has[RulesConfig] & Clock, Has[PermissionLogic]] = (
    for {
      clock <- ZIO.service[Clock.Service]
      cfg   <- ZIO.service[RulesConfig]
    } yield new PermissionLogicLive(cfg, clock)
  ).toLayer
}
