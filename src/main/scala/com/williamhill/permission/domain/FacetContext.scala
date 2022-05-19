package com.williamhill.permission.domain

import com.williamhill.permission.kafka.events.generic.Header

final case class FacetContext(
    header: Header,
    actions: List[Action], // TODO check if this needs to be a list or can be a single action
    playerId: PlayerId,
    universe: Universe,
    name: String,
    newStatuses: List[PermissionStatus],
    previousStatuses: List[PermissionStatus],
) {

  val denials: List[PermissionDenial] = actions.flatMap { action =>
    action.deniedPermissions.map { perm =>
      PermissionDenial(
        reason = action.reasonCode,
        description = action.denialDescription,
        permissionName = perm,
      )
    }
  }

  def addAction(action: Action): FacetContext = this.copy(actions = actions :+ action)

}
