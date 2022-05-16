package com.williamhill.permission

import com.williamhill.permission.application.config.{ActionDefinition, ActionsConfig}
import com.williamhill.permission.domain.{Action, FacetContext}
import zio.clock.Clock
import zio.{Has, Task, URLayer, ZIO}

import java.time.Instant

trait PermissionLogic {
  def enrichWithActions(facetContext: FacetContext): Task[FacetContext]
}

class PermissionLogicLive(actionsConfig: ActionsConfig, clock: Clock.Service) extends PermissionLogic {
  import com.williamhill.permission.PermissionLogic.*

  override def enrichWithActions(fc: FacetContext): Task[FacetContext] =
    clock.instant.map { now =>
      val applicableActions = actionsConfig.bindings
        .find(x => x.universe == fc.universe.value && x.eventType == fc.name && x.status == fc.newStatus.status)
        .toList
        .flatMap(_.actions)
        .flatMap(actionName => actionsConfig.definitions.filter(_.name == actionName))

      applicableActions.foldLeft(fc) { (context, actionDefinition) =>
        context.newStatus.endDate match {
          case Some(endDate) if endDate.isBefore(now) =>
            context.addAction(actionDefinition.toDomainWithDeadline(endDate))
          case _ => context.addAction(actionDefinition.toDomain)
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

  val layer: URLayer[Has[ActionsConfig] & Clock, Has[PermissionLogic]] = (
    for {
      clock <- ZIO.service[Clock.Service]
      cfg   <- ZIO.service[ActionsConfig]
    } yield new PermissionLogicLive(cfg, clock)
  ).toLayer
}
