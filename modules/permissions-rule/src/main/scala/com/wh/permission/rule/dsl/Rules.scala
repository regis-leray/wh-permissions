package com.wh.permission.rule.dsl

import cats.data.NonEmptySet
import com.wh.permission.rule.dsl.Expr.Export.*
import com.wh.permission.rule.dsl.Permission.*
import com.wh.permission.rule.dsl.errors.RuleError.InvalidAccountIdPath
import io.circe.Json
import io.circe.optics.JsonPath
import zio.{IO, ZIO}

import java.time.Instant

sealed trait Rule {
  type AccountId = String

  /** Apply all rules to a json payload, interrupt on first error raised
    *
    * @param json  payload
    * @param rules to apply
    * @return List of all accumulate successes or first error encounter when no accountId is found when it is a match (rule is true)
    */
  def run(
      json: Json,
  )(rules: NonEmptySet[PermissionRule]): IO[InvalidAccountIdPath, List[((AccountId, Facet, Permissions), PermissionRule)]] =
    ZIO
      .foreach(rules.toSortedSet.toList)(r => run1(json)(r).map(_.map(_.->(r))))
      .map(_.flatten)

  /** Apply 1 rule to a json payload, interrupt when a MissingPlayerId is not found
    *
    * @param json payload
    * @param rule to apply
    * @return Some value if rule is a perfect match, none value if no match
    *         In case the PermissionRule.accountId is invalid an error is emitted
    */
  def run1(json: Json)(rule: PermissionRule): IO[InvalidAccountIdPath, Option[(AccountId, Facet, Permissions)]] = for {
    is <- Runtime.run(rule.rule)(json).option.map(_.getOrElse(false))
    (facet, perms) = rule.permissions
    ret            = rule.accountId.string.getOption(json).map((_, facet, perms))

    value <- if (is) ZIO.fromEither(ret.toRight(InvalidAccountIdPath(json, rule)).map(Option(_))) else ZIO.none
  } yield value
}

object Rules extends Rule {

  object LifeTimeExceedRule extends PermissionRule("LifeTimeExceedRule") {
    val rule: Expr[Json, Boolean] = {
      val universeRule  = string($.header.universe).lowercase === "wh-mga"
      val eventTypeRule = string($.body.`type`).lowercase === "limit-exceeded-lifetime-deposit"
      universeRule && eventTypeRule
    }
    val accountId: JsonPath               = $.body.newValues.accountId
    val permissions: (Facet, Permissions) = Facet.Payment -> denyAll
  }

  object SelfExclusionRule extends PermissionRule("SelfExclusionRule") {
    val rule: Expr[Json, Boolean] = {
      val universeRule  = string($.header.universe).lowercase.oneOf("wh-mga", "wh-libertonia", "wh-eu-de", "wh-eu-dk")
      val eventTypeRule = string($.body.`type`).lowercase === "excluded"
      val endDate1      = ifPresent(instant($.body.newValues.exclusion.end).optional)(_ > Instant.now())
      val endDate2      = ifPresent(instant($.body.newValues.exclusion.realizedEnd).optional)(_ > Instant.now())

      universeRule && eventTypeRule && (endDate1 || endDate2)
    }
    val accountId: JsonPath               = $.body.newValues.playerId
    val permissions: (Facet, Permissions) = Facet.SelfExclusion -> deny(CanLoginWithPassword, CanWithdraw, CanDeposit, CanBet, CanGame)
  }

  object DormancyActiveRule extends PermissionRule("DormancyActiveRule") {
    val rule: Expr[Json, Boolean] = {
      val universeRule  = string($.header.universe).lowercase.oneOf("wh-mga", "wh-libertonia", "wh-eu-de", "wh-eu-dk")
      val eventTypeRule = string($.body.`type`).lowercase === "dormancy"
      val statusRule    = string($.body.newValues.data.status) === "dormant"

      universeRule && eventTypeRule && statusRule
    }
    val accountId: JsonPath               = $.body.newValues.id
    val permissions: (Facet, Permissions) = Facet.Dormancy -> deny(CanLoginWithPassword, CanBet, CanGame)
  }

  object DormancyInactiveRule extends PermissionRule("DormancyInactiveRule") {
    val rule: Expr[Json, Boolean] = {
      val universeRule  = string($.header.universe).lowercase.oneOf("wh-mga", "wh-libertonia", "wh-eu-de", "wh-eu-dk")
      val eventTypeRule = string($.body.`type`).lowercase === "dormancy"
      val statusRule    = string($.body.newValues.data.status) === "active"

      universeRule && eventTypeRule && statusRule
    }
    val accountId: JsonPath = $.body.newValues.id

    // TODO use Facet.Dormancy -> grant(CanLoginWithPassword, CanBet, CanGame)
    // Since grant(<specific_permission>) is not available, we are reactive all existing rules for now.
    val permissions: (Facet, Permissions) = Facet.Dormancy -> grantAll
  }

  object ProhibitionRule extends PermissionRule("ProhibitionRule") {
    val rule: Expr[Json, Boolean] = {
      val universeRule  = string($.header.universe).lowercase.oneOf("wh-mga", "wh-libertonia", "wh-eu-de", "wh-eu-dk")
      val eventTypeRule = string($.body.`type`).lowercase === "prohibition"
      val statusRule    = string($.body.newValues.data.status.`type`).lowercase === "prohibited"

      universeRule && eventTypeRule && statusRule
    }
    val accountId: JsonPath               = $.body.newValues.id
    val permissions: (Facet, Permissions) = Facet.Prohibition -> deny(CanLoginWithPassword, CanBet, CanGame)
  }

  object UnverifyProhibitionRule extends PermissionRule("UnverifyProhibitionRule") {
    val rule: Expr[Json, Boolean] = {
      val universeRule  = string($.header.universe).lowercase.oneOf("wh-mga", "wh-libertonia", "wh-eu-de", "wh-eu-dk")
      val eventTypeRule = string($.body.`type`).lowercase === "prohibition"
      val statusRule    = string($.body.newValues.data.status.`type`).lowercase === "allowed"

      universeRule && eventTypeRule && statusRule
    }
    val accountId: JsonPath               = $.body.newValues.id
    val permissions: (Facet, Permissions) = Facet.Prohibition -> deny(CanBet, CanGame)
  }

  object PaymentDepositLimitRule extends PermissionRule("PaymentDepositLimitRule") {
    val rule: Expr[Json, Boolean] = {
      val universeRule  = string($.header.universe).lowercase.oneOf("wh-eu-dk")
      val eventTypeRule = string($.body.`type`).lowercase === "limits_set"

      universeRule && eventTypeRule
    }
    val accountId: JsonPath               = $.body.newValues.accountId
    val permissions: (Facet, Permissions) = Facet.Payment -> grantAll
  }

  object PlayerRegistrationDepositLimitRequiredRule extends PermissionRule("PlayerRegistrationDepositLimitRequiredRule") {
    val rule: Expr[Json, Boolean] = {
      val universeRule  = string($.header.universe).lowercase.oneOf("wh-eu-dk")
      val eventTypeRule = string($.body.`type`).lowercase === "registered"

      val blockRule = ifPresent(string($.body.newValues.account.blockState.reason).optional)(_.length > 0)
      val closeRule = ifPresent(string($.body.newValues.account.closeState.reason).optional)(_.length > 0)

      universeRule && eventTypeRule && (blockRule || closeRule)
    }
    val accountId: JsonPath               = $.body.newValues.account.id
    val permissions: (Facet, Permissions) = Facet.Payment -> deny(CanWithdraw, CanBet, CanGame)
  }

  object CddThresholdBreachedRule extends PermissionRule("CddThresholdBreachedRule") {
    override val rule: Expr[Json, Boolean] = {
      val universeRule  = string($.header.universe).lowercase.oneOf("wh-mga")
      val eventTypeRule = string($.body.`type`).lowercase === "cdd-threshold-breached"

      universeRule && eventTypeRule
    }
    override val accountId: JsonPath               = $.body.newValues.accountId
    override val permissions: (Facet, Permissions) = Facet.Payment -> deny(CanWithdraw)
  }

  object CddThresholdAfterDeadlineRule extends PermissionRule("CddThresholdAfterDeadlineRule") {
    override val rule: Expr[Json, Boolean] = {
      val universeRule  = string($.header.universe).lowercase.oneOf("wh-mga")
      val eventTypeRule = string($.body.`type`).lowercase === "cdd-threshold-after-deadline"

      universeRule && eventTypeRule
    }
    override val accountId: JsonPath               = $.body.newValues.accountId
    override val permissions: (Facet, Permissions) = Facet.Payment -> denyAll
  }

  val All: NonEmptySet[PermissionRule] = NonEmptySet.of(
    LifeTimeExceedRule,
    SelfExclusionRule,
    DormancyActiveRule,
    DormancyInactiveRule,
    ProhibitionRule,
    UnverifyProhibitionRule,
    PaymentDepositLimitRule,
    PlayerRegistrationDepositLimitRequiredRule,
    CddThresholdBreachedRule,
    CddThresholdAfterDeadlineRule,
  )
}
