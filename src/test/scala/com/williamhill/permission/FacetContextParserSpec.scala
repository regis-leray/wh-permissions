package com.williamhill.permission

import java.time.Instant

import com.williamhill.permission.application.config.MappingsConfig
import com.williamhill.permission.domain.Fixtures.*
import com.williamhill.permission.kafka.events.generic.InputEvent
import com.williamhill.platform.event.common.Header
import com.williamhill.platform.event.permission.{FacetContext, PermissionStatus}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.{TableDrivenPropertyChecks, TableFor3}
import pureconfig.ConfigSource

class FacetContextParserSpec extends AnyFlatSpec with Matchers with TableDrivenPropertyChecks {

  behavior of "FacetContextParser"

  val scenarios: TableFor3[String, String, FacetContext] = {
    val universeWhMGA  = "wh-mga"
    val universeWhEUDK = "wh-eu-dk"
    Table(
      ("topic", "scenario", "expected result"),
      (
        "dormancy",
        "dormant",
        FacetContext(
          Header(
            id = "c321d02c-9544-4aca-ba6e-6ad404ea32c9",
            who = who(universeWhMGA),
            universe = universeWhMGA,
            when = "2022-02-28T14:22:41.433Z",
            traceContext = None,
          ),
          Vector.empty,
          "U00004334",
          universeWhMGA,
          "dormancy",
          PermissionStatus(Vector("Dormant")),
          Some(PermissionStatus(Vector("Active"))),
        ),
      ),
      (
        "prohibition",
        "prohibited",
        FacetContext(
          Header(
            id = "c321d02c-9544-4aca-ba6e-6ad404ea32c9",
            who = who(universeWhMGA),
            universe = universeWhMGA,
            when = "2022-01-31T13:22:41.454508Z",
            traceContext = None,
          ),
          Vector.empty,
          "U00004334",
          universeWhMGA,
          "prohibition",
          PermissionStatus(Vector("Prohibited")),
          Some(PermissionStatus(Vector("Allowed"))),
        ),
      ),
      (
        "excluded",
        "indefinite",
        FacetContext(
          Header(
            id = "c321d02c-9544-4aca-ba6e-6ad404ea32c9",
            who = who(universeWhEUDK),
            universe = universeWhEUDK,
            when = "2021-01-06T11:13:12.441799Z",
            traceContext = None,
          ),
          Vector.empty,
          "U00004335",
          universeWhEUDK,
          "excluded",
          PermissionStatus(Vector("indefinite"), Some(Instant.parse("2022-02-11T00:00:00Z"))),
          None,
        ),
      ),
      (
        "excluded",
        "permanent",
        FacetContext(
          Header(
            id = "c321d02c-9544-4aca-ba6e-6ad404ea32c9",
            who = who(universeWhMGA),
            universe = universeWhMGA,
            when = "2021-01-06T11:13:12.441799Z",
            traceContext = None,
          ),
          Vector.empty,
          "U00004336",
          universeWhMGA,
          "excluded",
          PermissionStatus(Vector("permanent"), Some(Instant.parse("2022-02-11T00:00:00Z"))),
          None,
        ),
      ),
      (
        "excluded",
        "temporary",
        FacetContext(
          Header(
            id = "c321d02c-9544-4aca-ba6e-6ad404ea32c9",
            who = who(universeWhMGA),
            universe = universeWhMGA,
            when = "2021-01-06T11:13:12.441799Z",
            traceContext = None,
          ),
          Vector.empty,
          "U00005335",
          universeWhMGA,
          "excluded",
          PermissionStatus(
            Vector("temporary"),
            Some(Instant.parse("2021-01-06T11:13:11.993Z")),
            Some(Instant.parse("2025-12-06T11:13:11.993Z")),
          ),
          None,
        ),
      ),
      (
        "excluded",
        "timeout",
        FacetContext(
          Header(
            id = "c321d02c-9544-4aca-ba6e-6ad404ea32c9",
            who = who(universeWhMGA),
            universe = universeWhMGA,
            when = "2021-01-06T11:13:12.441799Z",
            traceContext = None,
          ),
          Vector.empty,
          "U00005335",
          universeWhMGA,
          "excluded",
          PermissionStatus(
            Vector("timeout"),
            Some(Instant.parse("2021-01-06T11:13:11.993Z")),
            Some(Instant.parse("2021-02-07T11:13:11.993Z")),
          ),
          None,
        ),
      ),
    )
  }

  val parser: FacetContextParser = new FacetContextParser(ConfigSource.resources("mappings.conf").loadOrThrow[MappingsConfig])

  forAll(scenarios) { case (topic, scenario, expectedResult) =>
    it should s"convert $topic/$scenario input event to FacetContext" in {
      val inputEvent = FileReader.fromResources[InputEvent](s"functional-tests/$topic/in/$scenario.json")
      parser.parse(topic, inputEvent) shouldBe Right(expectedResult)
    }
  }
}
