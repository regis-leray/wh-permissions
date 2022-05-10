package com.williamhill.permission

import com.williamhill.permission.domain.Fixtures.*
import com.williamhill.permission.application.config.ActionsConfig
import com.williamhill.permission.domain.{Action, FacetContext, PermissionStatus}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import zio.ZIO
import zio.blocking.Blocking
import zio.clock.Clock

class PermissionsLogicSpec extends AnyFlatSpec with Matchers {

  behavior of "PermissionsLogic"

  it should "enrich FacetContext with actions based on the actions config" in {
    val facet = FacetContext(
      header("wh-eu-de"),
      Nil,
      playerId("U00000001"),
      universe("wh-eu-de"),
      "Dormancy",
      PermissionStatus("Dormant"),
      None,
    )

    val result = PermissionsLogic.enrichWithActions(facet).provideLayer((Blocking.live >>> ActionsConfig.layer) ++ Clock.live)

    runEffect(result).actions shouldBe List(
      Action(
        `type` = "notification",
        name = "sendDormancyNotification",
        reasonCode = "closed-dormant",
        denialDescription = "The account is closed as dormant",
        deniedPermissions = List("canLogin", "canBet", "canGame"),
      ),
    )
  }

  it should "not enrich anything when universe does not match with the one in the actions config" in {
    val facet = FacetContext(
      header("wh-foo"),
      Nil,
      playerId("U00000001"),
      universe("wh-foo"),
      "Dormancy",
      PermissionStatus("Dormant"),
      None,
    )

    val result = PermissionsLogic.enrichWithActions(facet).provideLayer((Blocking.live >>> ActionsConfig.layer) ++ Clock.live)

    runEffect(result).actions shouldBe Nil
  }

  def runEffect[E, A](effect: ZIO[Any, E, A]): A =
    zio.Runtime.global.unsafeRun(effect)
}
