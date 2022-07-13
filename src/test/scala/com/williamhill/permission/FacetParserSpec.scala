package com.williamhill.permission

import java.time.Instant

import com.williamhill.permission.config.MappingsConfig
import com.williamhill.permission.kafka.events.generic.InputEvent
import com.williamhill.platform.event.common.Header
import com.williamhill.platform.event.common.Header.Who
import com.williamhill.platform.event.permission.{FacetContext, PermissionStatus}
import pureconfig.ConfigSource
import zio.test.Assertion.*
import zio.test.environment.TestEnvironment
import zio.test.{DefaultRunnableSpec, ZSpec, assert}

object FacetParserSpec extends DefaultRunnableSpec {
  val parser: FacetContextParser = new FacetContextParser(ConfigSource.resources("mappings.conf").loadOrThrow[MappingsConfig])

  def anonymous(universe: String) = Who(
    id = "-1",
    `type` = Header.Who.Type.Program,
    name = "anonymous",
    ip = Some("127.0.0.1"),
    sessionId = None,
    universe = Some(universe),
    allowedUniverses = Some(List(universe)),
  )

  override def spec: ZSpec[TestEnvironment, Any] = suite("FacetParserSpec")(
    test("prohibition/prohibited") {
      val expected = FacetContext(
        Header(
          id = "c321d02c-9544-4aca-ba6e-6ad404ea32c9",
          traceContext = None,
          who = anonymous("wh-mga"),
          universe = "wh-mga",
          when = "2022-07-08T15:41:13.217051Z",
        ),
        Vector.empty,
        "U000002Z3C",
        "wh-mga",
        "prohibition",
        PermissionStatus(Vector("prohibited")),
        Some(PermissionStatus(Vector("allowed"))),
      )

      val inputEvent = FileReader.fromResources[InputEvent](s"functional-tests/prohibition/in/prohibited.json")
      assert(parser.parse("prohibition", inputEvent))(isRight(equalTo(expected)))
    },
    test("dormancy") {
      val expected = FacetContext(
        Header(
          id = "c321d02c-9544-4aca-ba6e-6ad404ea32c9",
          who = anonymous("wh-mga"),
          universe = "wh-mga",
          when = "2022-02-28T14:22:41.433Z",
          traceContext = None,
        ),
        Vector.empty,
        "U00004334",
        "wh-mga",
        "dormancy",
        PermissionStatus(Vector("dormant")),
        Some(PermissionStatus(Vector("active"))),
      )

      val inputEvent = FileReader.fromResources[InputEvent](s"functional-tests/dormancy/in/dormant.json")
      assert(parser.parse("dormancy", inputEvent))(isRight(equalTo(expected)))
    },
    test("excluded/indefinite") {
      val expected = FacetContext(
        Header(
          id = "c321d02c-9544-4aca-ba6e-6ad404ea32c9",
          who = anonymous("wh-eu-dk"),
          universe = "wh-eu-dk",
          when = "2021-01-06T11:13:12.441799Z",
          traceContext = None,
        ),
        Vector.empty,
        "U00004335",
        "wh-eu-dk",
        "excluded",
        PermissionStatus(Vector("indefinite"), Some(Instant.parse("2022-02-11T00:00:00Z"))),
        None,
      )

      val inputEvent = FileReader.fromResources[InputEvent](s"functional-tests/excluded/in/indefinite.json")
      assert(parser.parse("excluded", inputEvent))(isRight(equalTo(expected)))
    },
    test("excluded/permanent") {
      val expected = FacetContext(
        Header(
          id = "c321d02c-9544-4aca-ba6e-6ad404ea32c9",
          who = anonymous("wh-mga"),
          universe = "wh-mga",
          when = "2021-01-06T11:13:12.441799Z",
          traceContext = None,
        ),
        Vector.empty,
        "U00004336",
        "wh-mga",
        "excluded",
        PermissionStatus(Vector("permanent"), Some(Instant.parse("2022-02-11T00:00:00Z"))),
        None,
      )
      val inputEvent = FileReader.fromResources[InputEvent](s"functional-tests/excluded/in/permanent.json")
      assert(parser.parse("excluded", inputEvent))(isRight(equalTo(expected)))
    },
    test("excluded/temporary") {
      val expected = FacetContext(
        Header(
          id = "c321d02c-9544-4aca-ba6e-6ad404ea32c9",
          who = anonymous("wh-mga"),
          universe = "wh-mga",
          when = "2021-01-06T11:13:12.441799Z",
          traceContext = None,
        ),
        Vector.empty,
        "U00005335",
        "wh-mga",
        "excluded",
        PermissionStatus(
          Vector("temporary"),
          Some(Instant.parse("2021-01-06T11:13:11.993Z")),
          Some(Instant.parse("2025-12-06T11:13:11.993Z")),
        ),
        None,
      )

      val inputEvent = FileReader.fromResources[InputEvent](s"functional-tests/excluded/in/temporary.json")
      assert(parser.parse("excluded", inputEvent))(isRight(equalTo(expected)))
    },
    test("payments/deposit-lifetime-limit-exceed") {
      val expected = FacetContext(
        Header(
          id = "1b00ec09-cc5d-4ac9-ba0c-a5612b63aafd",
          who = anonymous("wh-mga"),
          universe = "wh-mga",
          when = "2021-12-08T20:03:55.812585Z",
          traceContext = None,
        ),
        Vector.empty,
        "EXW",
        "wh-mga",
        "limit-exceeded-lifetime-deposit",
        PermissionStatus(
          Vector("activated"),
          None,
          None,
        ),
        None,
      )

      val inputEvent = FileReader.fromResources[InputEvent](s"functional-tests/payments_events_v2/in/lifetime-limit-exceeded.json")
      assert(parser.parse("payments_events_v2", inputEvent))(isRight(equalTo(expected)))
    },
    test("payments_limits_v1/deposit-limit") {
      val expected = FacetContext(
        Header(
          id = "e74f3c1f-b669-43b7-81be-6be0e31cedf8",
          who = anonymous("wh-eu-dk"),
          universe = "wh-eu-dk",
          when = "2021-01-06T11:13:12.021575Z",
          traceContext = None,
        ),
        Vector.empty,
        "U00000S68F",
        "wh-eu-dk",
        "deposit-limits-set",
        PermissionStatus(
          Vector("activated"),
          None,
          None,
        ),
        Some(
          PermissionStatus(
            Vector("activated"),
            None,
            None,
          ),
        ),
      )

      val inputEvent = FileReader.fromResources[InputEvent](s"functional-tests/payments_limits_v1/in/deposit-limit-set.json")
      assert(parser.parse("payments_limits_v1", inputEvent))(isRight(equalTo(expected)))
    },
    test("player_events_v4/register") {
      val expected = FacetContext(
        Header(
          id = "e74f3c1f-b669-43b7-81be-6be0e31cedf8",
          who = anonymous("wh-eu-dk"),
          universe = "wh-eu-dk",
          when = "2021-01-06T11:13:12.021575Z",
          traceContext = None,
        ),
        Vector.empty,
        "b7c7af4f-7571-40af-92a2-9c12b218ad73",
        "wh-eu-dk",
        "player-account-registered",
        PermissionStatus(
          Vector("blocked", "closed"),
          None,
          None,
        ),
        None,
      )

      val inputEvent = FileReader.fromResources[InputEvent](s"functional-tests/player_events_v4/in/registered-wh-eu-dk.json")
      assert(parser.parse("player_events_v4", inputEvent))(isRight(equalTo(expected)))
    },
  )
}
