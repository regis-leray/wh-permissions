package com.williamhill.permission.domain

import com.williamhill.permission.kafka.events.generic.Header

final case class FacetContext(
    header: Header,
    actions: List[Action], // TODO check if this needs to be a list or can be a single action
    playerId: PlayerId,
    universe: Universe,
    name: String,
    newStatus: PermissionStatus,             // extracted from newValues
    previousStatus: Option[PermissionStatus],// extracted from previousValues
) {

  val denials: List[PermissionDenial] = actions.flatMap { case Action(_, reason, perms, _, denialDescription) =>
    perms.map { perm =>
      PermissionDenial(
        reason = reason,
        description = denialDescription.getOrElse(s"Permission not granted because of: $reason"),
        permissionName = perm,
      )
    }
  }

  def addAction(action: Action): FacetContext = this.copy(actions = actions :+ action)

}
