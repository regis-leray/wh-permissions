package com.wh.permission.rule.dsl

import com.wh.permission.rule.dsl.Expr.Export.*
import com.wh.permission.rule.dsl.Permission.{Permissions, deny}
import io.circe.Json
import io.circe.optics.JsonPath

object Demo {
  def main(args: Array[String]): Unit = {

    val json: Json = io.circe.parser
      .parse("""
      |{
      |   "header":{
      |      "id":"1b00ec09-cc5d-4ac9-ba0c-a5612b63aafd",
      |      "universe" : "WH-MGA",
      |      "when":"2021-12-08T20:03:55.812585Z",
      |      "who":{
      |         "id":"-1",
      |         "name":"anonymous",
      |         "type":"program",
      |         "ip":"127.0.0.1"
      |      },
      |      "allowUniverses" : ["wh-mga", "wh-us", "wh-eur"]
      |   },
      |   "body":{
      |      "type":"limit-exceeded-lifetime-deposit",
      |      "newValues":{
      |         "detailedPaymentType":"DJgvTAmaL",
      |         "provider":"ZiSziKoOiZ",
      |         "paymentStatus":"authorised",
      |         "maskedAccountId":"669703******2782",
      |         "providerFeeBaseCurrency":"GIP",
      |         "paymentType":"deposit",
      |         "amount":400,
      |         "lifetimeSummary":{
      |            "deposits":{
      |               "pendingTotal":1000,
      |               "pendingCount":1,
      |               "completedTotal":2000,
      |               "completedCount":5
      |            },
      |            "withdrawals":{
      |               "pendingTotal":0,
      |               "pendingCount":0,
      |               "completedTotal":1500,
      |               "completedCount":4
      |            }
      |         },
      |         "creationDate":"2021-12-08T20:03:55.803Z",
      |         "currency":"TMT",
      |         "fee":80,
      |         "paymentReference":"mbvGilXud",
      |         "accountId":"EXW",
      |         "providerFeeBase":30,
      |         "providerService":"u0WhLWOpIK4z0mTjVf5SERj8KKsgZaXWhIZqN3ebr2q2Sp2hFIFzpWEgx4utVo8awQ2v9",
      |         "universe":"wh-mga",
      |
      |         "account" : {
      |           "blockState" : { "reason" : "blocked" },
      |           "closeState" : { "reason" : "closed" }
      |         }
      |      }
      |   }
      |}
      """.stripMargin)
      .toOption
      .get

    println(
      zio.Runtime.default.unsafeRun(
        Rules.run1(json)(LifeTimeExceedPermissionRule),
        // Runtime.run(LifeTimeExceedPermissionRule.rule)(json),
      ),
    )
  }
}

object LifeTimeExceedPermissionRule extends PermissionRule("Test") {

  val rule: Expr[Json, Boolean] = {
    val universeRule       = string($.header.universe).lowercase === "wh-mga"
    val allowUniversesRule = strings($.header.allowUniverses).allOf("wh-mga", "wh-eur")
    // val allowUniversesRule = strings($.header.allowUniverses) === List("wh-mga", "wh-us", "wh-eur")

    val eventTypeRule = string($.body.`type`) === "limit-exceeded-lifetime-deposit"
    val amountRule    = int($.body.newValues.amount) <= 400
    val blockedRule   = ifPresent(string($.body.newValues.account.blockState.reason).optional)(_ === "blocked")
    val closedRule    = ifPresent(string($.body.newValues.account.closeState.reason).optional)(_ === "closed")

    (universeRule && allowUniversesRule && eventTypeRule && amountRule) && (blockedRule || closedRule)
  }

  val accountId: JsonPath = $.body.newValues.accountId

  val permissions: (Facet, Permissions) = Facet.Payment -> deny(Permission.CanVerify, Permission.CanBet)
}
