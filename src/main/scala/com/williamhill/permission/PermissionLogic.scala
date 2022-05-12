package com.williamhill.permission

import com.williamhill.permission.application.config.{ActionDefinition, ActionsConfig}
import com.williamhill.permission.domain.{Action, FacetContext}
import zio.clock.Clock
import zio.{Has, ULayer, URIO, ZIO}

import java.time.Instant

trait PermissionLogic {
  def enrichWithActions(facetContext: FacetContext): URIO[Has[ActionsConfig] & Has[Clock.Service], FacetContext]
}

object PermissionLogic {

  implicit final private class ActionDefinitionSyntax(private val ad: ActionDefinition) extends AnyVal {
    def toDomain: Action =
      Action(ad.`type`, ad.name, ad.reasonCode, ad.denialDescription, ad.deniedPermissions)

    def toDomainWithDeadline(deadline: Instant): Action =
      Action(ad.`type`, ad.name, ad.reasonCode, ad.denialDescription, ad.deniedPermissions, Some(deadline))
  }

  val layer: ULayer[Has[PermissionLogic]] = ZIO
    .succeed(new PermissionLogic() {
      override def enrichWithActions(fc: FacetContext): URIO[Has[ActionsConfig] & Has[Clock.Service], FacetContext] =
        ZIO
          .service[ActionsConfig]
          .zip(ZIO.serviceWith[Clock.Service](_.instant))
          .map { case (actionConfig, now) =>
            val applicableActions = actionConfig.bindings
              .find(x => x.universe == fc.universe.value && x.eventType == fc.name && x.status == fc.newStatus.status)
              .toList
              .flatMap(_.actions)
              .flatMap(actionName => actionConfig.definitions.filter(_.name == actionName))

            applicableActions.foldLeft(fc) { (context, actionDefinition) =>
              context.newStatus.endDate match {
                case Some(endDate) if endDate.isBefore(now) =>
                  context.addAction(actionDefinition.toDomainWithDeadline(endDate))
                case _ => context.addAction(actionDefinition.toDomain)
              }
            }
          }
    })
    .toLayer
}
