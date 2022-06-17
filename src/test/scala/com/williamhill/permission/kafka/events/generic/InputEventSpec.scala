package com.williamhill.permission.kafka.events.generic

import java.time.Instant

import com.typesafe.scalalogging.LazyLogging
import io.circe.Printer.spaces2
import io.circe.parser.{decode, parse}
import io.circe.syntax.*
import zio.test.Assertion.*
import zio.test.{DefaultRunnableSpec, assert}

object InputEventSpec extends DefaultRunnableSpec with LazyLogging {

  val inputEvent = InputEvent(
    InputHeader(
      id = "1",
      who = Who(id = "-1", name = "anonymous", `type` = "program", ip = Some("100.77.100.167")),
      universe = "wh-eu-de",
      when = Instant.parse("2022-02-28T14:22:41.433Z"),
      sessionId = Some("5848afcd-8020-11ec-a1d6-5057d25f6201"),
      traceId = None,
    ),
    parse("""{
            |  "type": "Dormancy",
            |  "newValues": {
            |   "id": "U00004334",
            |   "universe": "wh-eu-de",
            |    "data": {
            |        "status": "Dormant"
            |     }
            |    }
            |}""".stripMargin).toOption.get,
  )
  val jsonStrict =
    """{
      |  "header": {
      |    "id": "1",
      |     "universe": "wh-eu-de",
      |    "who": {
      |      "id": "-1",
      |      "name": "anonymous",
      |      "type": "program",
      |      "ip": "100.77.100.167"
      |    },
      |    "when": "2022-02-28T14:22:41.433Z",
      |    "sessionId": "5848afcd-8020-11ec-a1d6-5057d25f6201",
      |    "traceId": null
      |  },
      |  "body": {
      |    "type": "Dormancy",
      |    "newValues": {
      |      "id": "U00004334",
      |      "universe": "wh-eu-de",
      |      "data": {
      |        "status": "Dormant"
      |      }
      |    }
      |  }
      |}
      |""".stripMargin

  val jsonWithTrailingCharacters = "�asdasd" + jsonStrict + "�asdasd"

  def spec =
    suite("InputEvent")(
      test("deserialized from strict json") {

        val parsed = decode[InputEvent](jsonStrict)
          .fold(
            pf => {
              println(pf)
              None
            },
            Some(_),
          )
          .get

        assert(parsed)(equalTo(inputEvent))
      },
      test("serialized from strict json") {
        val jsonString2 = inputEvent.asJson.printWith(spaces2)
        val parsed = decode[InputEvent](jsonString2)
          .fold(
            pf => {
              println(pf)
              None
            },
            Some(_),
          )
          .get
        assert(parsed)(equalTo(inputEvent))

      },
      test("clean method should work ") {
        val dirty    = """sadsada { "test":"a" } dsadsadsadas """
        val expected = """{ "test":"a" }"""
        val result   = InputEvent.clean(dirty)
        assert(result)(equalTo(expected))

      },
      test("deserialized from jsonstring with") {

        val cleanString = InputEvent.clean(jsonWithTrailingCharacters)

        val parsedClean = decode[InputEvent](cleanString)
          .fold(
            pf => {
              println(pf)
              None
            },
            Some(_),
          )
          .get
        val parsed = decode[InputEvent](jsonStrict)
          .fold(
            pf => {
              println(pf)
              None
            },
            Some(_),
          )
          .get
        assert(parsedClean)(equalTo(parsed))
      },
    )
}
