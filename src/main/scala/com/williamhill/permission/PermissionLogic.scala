package com.williamhill.permission

import cats.syntax.either.*
import cats.syntax.traverse.*
import com.williamhill.permission.application.AppError
import com.williamhill.permission.application.config.RulesConfig
import com.williamhill.permission.dsl.ExpressionEvaluator
import com.williamhill.platform.event.permission.{Action, FacetContext}
import io.circe.{Json, JsonObject}
import zio.clock.Clock
import zio.{Has, IO, URLayer, ZIO}

trait PermissionLogic {
  def enrichWithActions(facetContext: FacetContext): IO[AppError, FacetContext]
}

class PermissionLogicLive(config: RulesConfig) extends PermissionLogic {

  override def enrichWithActions(fc: FacetContext): IO[AppError, FacetContext] =
    ZIO.fromEither {
      val evaluator = new ExpressionEvaluator(
        Json
          .fromJsonObject(
            JsonObject(
              "universe" -> Json.fromString(fc.universe),
              "event"    -> Json.fromString(fc.name),
              "statuses" -> Json.arr(fc.newStatus.statuses.map(Json.fromString)*),
            ),
          )
          .hcursor,
      )

      config.rules
        .flatTraverse(evaluator.evaluateVector[String])
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
                fc.newStatus.endDate,
              ),
            ),
        )
        .bimap(e => AppError.fromDecodingFailure(e), actions => fc.copy(actions = actions))
    }

}

object PermissionLogic {
  val layer: URLayer[Has[RulesConfig] & Clock, Has[PermissionLogic]] =
    ZIO.service[RulesConfig].map(new PermissionLogicLive(_)).toLayer
}
