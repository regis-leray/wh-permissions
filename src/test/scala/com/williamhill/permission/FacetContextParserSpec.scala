package com.williamhill.permission

import java.time.Instant

import com.williamhill.permission.application.config.MappingsConfig
import com.williamhill.permission.domain.Fixtures.*
import com.williamhill.permission.domain.{FacetContext, PermissionStatus, PlayerId}
import com.williamhill.permission.kafka.events.generic.{Header, InputEvent}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.{TableDrivenPropertyChecks, TableFor3}
import pureconfig.ConfigSource

class FacetContextParserSpec extends AnyFlatSpec with Matchers with TableDrivenPropertyChecks {

  behavior of "FacetContextParser"

  val scenarios: TableFor3[String, String, FacetContext] =
    Table(
      ("topic", "scenario", "expected result"),
      (
        "dormancy",
        "dormant",
        FacetContext(
          Header(
            "c321d02c-9544-4aca-ba6e-6ad404ea32c9",
            who,
            "wh-mga",
            Instant.parse("2022-02-28T14:22:41.433Z"),
            Some("5848afcd-8020-11ec-a1d6-5057d25f6201"),
            None,
          ),
          Vector.empty,
          PlayerId("U00004334"),
          universe("wh-mga"),
          "dormancy",
          Vector(PermissionStatus("Dormant")),
          Vector(PermissionStatus("Active")),
        ),
      ),
      (
        "prohibition",
        "prohibited",
        FacetContext(
          Header(
            "c321d02c-9544-4aca-ba6e-6ad404ea32c9",
            who,
            "wh-mga",
            Instant.parse("2022-01-31T13:22:41.454508Z"),
            Some("5848afcd-8020-11ec-a1d6-5057d25f6201"),
            None,
          ),
          Vector.empty,
          PlayerId("U00004334"),
          universe("wh-mga"),
          "prohibition",
          Vector(PermissionStatus("Prohibited")),
          Vector(PermissionStatus("Allowed")),
        ),
      ),
      (
        "excluded",
        "indefinite",
        FacetContext(
          Header(
            "c321d02c-9544-4aca-ba6e-6ad404ea32c9",
            who,
            "wh-eu-dk",
            Instant.parse("2021-01-06T11:13:12.441799Z"),
            Some("445b4e38-84c1-11ec-a8a3-0242ac120002"),
            Some("48dac344-84c1-11ec-a8a3-0242ac120002"),
          ),
          Vector.empty,
          PlayerId("U00004335"),
          universe("wh-eu-dk"),
          "excluded",
          Vector(PermissionStatus("indefinite", Some(Instant.parse("2022-02-11T00:00:00Z")))),
          Vector.empty,
        ),
      ),
      (
        "excluded",
        "permanent",
        FacetContext(
          Header(
            "c321d02c-9544-4aca-ba6e-6ad404ea32c9",
            who,
            "wh-mga",
            Instant.parse("2021-01-06T11:13:12.441799Z"),
            Some("445b4e38-84c1-11ec-a8a3-0242ac120002"),
            Some("48dac344-84c1-11ec-a8a3-0242ac120002"),
          ),
          Vector.empty,
          PlayerId("U00004336"),
          universe("wh-mga"),
          "excluded",
          Vector(PermissionStatus("permanent", Some(Instant.parse("2022-02-11T00:00:00Z")))),
          Vector.empty,
        ),
      ),
      (
        "excluded",
        "temporary",
        FacetContext(
          Header(
            "c321d02c-9544-4aca-ba6e-6ad404ea32c9",
            who,
            "wh-mga",
            Instant.parse("2021-01-06T11:13:12.441799Z"),
            Some("445b4e38-84c1-11ec-a8a3-0242ac120002"),
            Some("48dac344-84c1-11ec-a8a3-0242ac120002"),
          ),
          Vector.empty,
          PlayerId("U00005335"),
          universe("wh-mga"),
          "excluded",
          Vector(
            PermissionStatus("temporary", Some(Instant.parse("2021-01-06T11:13:11.993Z")), Some(Instant.parse("2021-12-06T11:13:11.993Z"))),
          ),
          Vector.empty,
        ),
      ),
      (
        "excluded",
        "timeout",
        FacetContext(
          Header(
            "c321d02c-9544-4aca-ba6e-6ad404ea32c9",
            who,
            "wh-mga",
            Instant.parse("2021-01-06T11:13:12.441799Z"),
            Some("445b4e38-84c1-11ec-a8a3-0242ac120002"),
            Some("48dac344-84c1-11ec-a8a3-0242ac120002"),
          ),
          Vector.empty,
          PlayerId("U00005335"),
          universe("wh-mga"),
          "excluded",
          Vector(
            PermissionStatus("timeout", Some(Instant.parse("2021-01-06T11:13:11.993Z")), Some(Instant.parse("2021-02-07T11:13:11.993Z"))),
          ),
          Vector.empty,
        ),
      ),
    )

  val parser: FacetContextParser = new FacetContextParser(ConfigSource.resources("mappings.conf").loadOrThrow[MappingsConfig])

  forAll(scenarios) { case (topic, scenario, expectedResult) =>
    it should s"convert $topic/$scenario input event to FacetContext" in {
      val inputEvent = FileReader.fromResources[InputEvent](s"functional-tests/$topic/in/$scenario.json")
      parser.parse(topic, inputEvent) shouldBe Right(expectedResult)
    }
  }
}
