package com.williamhill.permission

import java.time.Instant

import com.williamhill.permission.domain.{Action, ActionsPerUniverse, FacetContext}

object PermissionsLogic {

  def processSingleAction(action: Action, facetContext: FacetContext): FacetContext =
    facetContext.status.maybeEndDate match {
      case Some(endDate) if endDate.isBefore(Instant.now()) =>
        facetContext.addAction(action.withDeadline(endDate))

      case _ => facetContext.addAction(action)

    }

  def processActions(actions: List[Action], facetContext: FacetContext): FacetContext =
    actions.foldLeft(facetContext)((context, action) => processSingleAction(action, context))

  def process(facetContext: FacetContext): FacetContext = {
    val actions = ActionsPerUniverse.find(
      universe = facetContext.universe.toString.toLowerCase(),
      event = facetContext.name,
      status = facetContext.status.status,
    )
    processActions(actions, facetContext)
  }

}
