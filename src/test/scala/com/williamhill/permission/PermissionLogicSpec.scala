package com.williamhill.permission

import com.williamhill.permission.application.config.RulesConfig
import com.williamhill.permission.domain.Fixtures.{header, universe}
import com.williamhill.permission.domain.{Action, FacetContext, PermissionStatus, PlayerId}
import zio.test.Assertion.*
import zio.test.environment.TestEnvironment
import zio.test.{DefaultRunnableSpec, ZSpec, assert}
import zio.{Has, RLayer, ZEnv, ZIO}

object PermissionLogicSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] = {
    suite("Calculation of permissions")(
      testM("enrich FacetContext with actions from actions cfg") {
        val facetContext = FacetContext(
          header("wh-eu-de"),
          Vector.empty,
          PlayerId("U00000001"),
          universe("wh-eu-de"),
          "dormancy",
          PermissionStatus(Vector("Dormant")),
          None,
        )

        val expectedEnrichedContext = facetContext.copy(actions =
          Vector(
            Action(
              `type` = "notification",
              name = "sendDormancyNotification",
              reasonCode = "closed-dormant",
              denialDescription = "The account is closed as dormant",
              deniedPermissions = List("canLogin", "canBet", "canGame"),
            ),
          ),
        )
        (for {
          permissionLogic    <- ZIO.service[PermissionLogic]
          contextWithActions <- permissionLogic.enrichWithActions(facetContext)
        } yield assert(contextWithActions)(equalTo(expectedEnrichedContext)))
          .provideLayer(Internal.configLayer)
      },
      testM("not enrich anything when universe does not match with the one in the actions config") {
        val facet = FacetContext(
          header("wh-foo"),
          Vector.empty,
          PlayerId("U00000001"),
          universe("wh-foo"),
          "dormancy",
          PermissionStatus(Vector("Dormant")),
          None,
        )

        (for {
          permissionLogic <- ZIO.service[PermissionLogic]
          resultContext   <- permissionLogic.enrichWithActions(facet)
        } yield assert(resultContext)(equalTo(facet)))
          .provideLayer(Internal.configLayer)
      },
    )

  }
  object Internal {
    val configLayer: RLayer[ZEnv, ZEnv & Has[RulesConfig] & Has[PermissionLogic]] =
      ZEnv.live >+> RulesConfig.layer >+> PermissionLogic.layer
  }

}
