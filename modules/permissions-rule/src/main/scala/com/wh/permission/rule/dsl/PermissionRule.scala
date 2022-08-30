package com.wh.permission.rule.dsl

import cats.Order
import cats.data.NonEmptySet
import com.wh.permission.rule.dsl.Permission.Permissions
import io.circe.Json
import io.circe.optics.JsonPath

sealed abstract class Permission(val name: String)

object Permission {
  final case object CanLoginWithPassword extends Permission("canLoginWithPassword")
  final case object CanBet               extends Permission("canBet")
  final case object CanDeposit           extends Permission("canDeposit")
  final case object CanWithdraw          extends Permission("canWithdraw")
  final case object CanResetPassword     extends Permission("canResetPassword")
  final case object CanVerify            extends Permission("canVerify")
  final case object CanGame              extends Permission("canGame")
  final case object CanGetBonus          extends Permission("canGetBonus")

  implicit val order: Order[Permission] = (x: Permission, y: Permission) => x.name.compareTo(y.name)

  val All: NonEmptySet[Permission] =
    NonEmptySet.of(CanLoginWithPassword, CanBet, CanDeposit, CanWithdraw, CanResetPassword, CanVerify, CanGame, CanGetBonus)

  /** Permissions represents Deny permissions or Grant Permissions
    *
    * Either.Left  defines deny permissions
    * Either.Right defines grant permissions
    */
  type Permissions = Either[NonEmptySet[Permission], NonEmptySet[Permission]]

  def granted(p: Permissions): Set[Permission] = p match {
    case Left(l) if l.diff(Permission.All).nonEmpty => l.diff(Permission.All)
    case Right(l)                                   => l.toSortedSet
    case _                                          => Set.empty
  }

  def denied(p: Permissions): Set[Permission] = p match {
    case Right(l) if l.diff(Permission.All).nonEmpty => l.diff(Permission.All)
    case Left(l)                                     => l.toSortedSet
    case _                                           => Set.empty
  }

  val grantAll: Permissions = Right(Permission.All)

  val denyAll: Permissions = deny(Permission.All)

  /** Ensure backward compatibility we don't allow to grant specific permissions until permission state is available
    */
  // def grant(set: NonEmptySet[Permission]): Permissions = Right(set)

  /** Ensure backward compatibility we don't allow to grant specific permissions until permission state is available
    */
  // def grant(p: Permission, other: Permission*): Permissions = Right(NonEmptySet.of(p, other: _*))

  def deny(p: Permission, other: Permission*): Permissions = Left(NonEmptySet.of(p, other: _*))

  def deny(set: NonEmptySet[Permission]): Permissions = Left(set)
}

sealed abstract class Facet(val name: String)

//TODO validate the name of the facet to use !
object Facet {
  final case object Player        extends Facet("Player")
  final case object Payment       extends Facet("Payment")
  final case object Wallet        extends Facet("Wallet")
  final case object Dormancy      extends Facet("Dormancy")
  final case object Kyc           extends Facet("Kyc")
  final case object Prohibition   extends Facet("Prohibition")
  final case object SelfExclusion extends Facet("SelfExclusion")
}

abstract class PermissionRule(val name: String) {
  val rule: Expr[Json, Boolean]
  val accountId: JsonPath
  val permissions: (Facet, Permissions)
}

object PermissionRule {
  implicit val order: Order[PermissionRule] = (x: PermissionRule, y: PermissionRule) => x.name.compareTo(y.name)
}
