package com.williamhill.permission

import java.time.Instant

import com.williamhill.permission.application.config.{ActionDefinition, RulesConfig}
import com.williamhill.permission.domain.{Action, FacetContext}
import zio.clock.Clock
import zio.{Has, Task, URLayer, ZIO}

trait PermissionLogic {
  def enrichWithActions(facetContext: FacetContext): Task[FacetContext]
}

class PermissionLogicLive(actionsConfig: RulesConfig, clock: Clock.Service) extends PermissionLogic {
  import com.williamhill.permission.PermissionLogic.*

  override def enrichWithActions(fc: FacetContext): Task[FacetContext] =
    clock.instant.map { now =>
      actionsConfig.rules
        .filter(b => b.universe == fc.universe.value && b.eventType == fc.name)
        .flatMap(b =>
          fc.newStatuses
            .find(_.status == b.status)
            .map(_ -> actionsConfig.actions.filter(a => b.actions.contains(a.name))),
        )
        .foldLeft(fc) { case (context, (status, actions)) =>
          status.endDate match {
            case Some(endDate) if endDate.isBefore(now) =>
              actions.foldLeft(context)(_ addAction _.toDomainWithDeadline(endDate))
            case _ =>
              actions.foldLeft(context)(_ addAction _.toDomain)
          }
        }
    }

}

object PermissionLogic {
  implicit final class ActionDefinitionSyntax(private val ad: ActionDefinition) extends AnyVal {
    def toDomain: Action =
      Action(ad.`type`, ad.name, ad.reasonCode, ad.denialDescription, ad.deniedPermissions)

    def toDomainWithDeadline(deadline: Instant): Action =
      Action(ad.`type`, ad.name, ad.reasonCode, ad.denialDescription, ad.deniedPermissions, Some(deadline))
  }

  val layer: URLayer[Has[RulesConfig] & Clock, Has[PermissionLogic]] = (
    for {
      clock <- ZIO.service[Clock.Service]
      cfg   <- ZIO.service[RulesConfig]
    } yield new PermissionLogicLive(cfg, clock)
  ).toLayer
}
