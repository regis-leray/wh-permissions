package com.williamhill.permission.domain

import java.time.Instant

import com.williamhill.permission.kafka.events.generic.Header
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

final case class OutputAction(name: String, relatesToPermissions: List[String], `type`: String, deadline: Option[Instant])

object OutputAction {
  implicit val codec: Codec[OutputAction] = deriveCodec

  def apply(action: Action): OutputAction = OutputAction(action.name, action.deniedPermissions, action.`type`, action.deadline)
}

final case class FacetContext(
    header: Header,
    actions: Vector[Action],
    playerId: PlayerId,
    universe: Universe,
    name: String,
    newStatus: PermissionStatus,
    previousStatus: Option[PermissionStatus],
) {

  val denials: Map[String, Vector[PermissionDenial]] = actions.flatMap { action =>
    action.deniedPermissions.map { perm =>
      perm -> Vector(
        PermissionDenial(
          reasonCode = action.reasonCode,
          description = action.denialDescription,
        ),
      )
    }
  }.toMap

  val outputActions: Vector[OutputAction] = actions.map(OutputAction.apply)

  def addAction(action: Action): FacetContext = this.copy(actions = actions :+ action)

}
