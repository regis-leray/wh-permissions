package com.williamhill.permission

import com.wh.permission.rule.dsl.Permission
import com.wh.permission.rule.dsl.Permission.Permissions

object PermissionState {
  type DeniedPermission = Set[Permission]

  def compute(s: State, i: (String, Permissions)): (State, DeniedPermission) = {
    val (key, permissions) = i

    val newState = s.updatedWith(key) {
      case Some(current) => Some(compute1(current, permissions))
      case None          => Some(compute1(Set.empty, permissions))
    }

    newState -> combine(newState)
  }

  private def compute1(current: DeniedPermission, permissions: Permissions): DeniedPermission = permissions match {
    case Left(denied)   => current ++ denied.toSortedSet
    case Right(granted) => current.diff(granted.toSortedSet)
  }

  private def combine(s: State): DeniedPermission =
    s.foldLeft(Set.empty[Permission]) { case (s, (_, p)) => s ++ p }
}
