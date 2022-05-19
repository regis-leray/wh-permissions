package com.williamhill.permission

import java.time.Instant

import com.williamhill.permission.application.config.MappingsConfig
import com.williamhill.permission.domain.Fixtures.*
import com.williamhill.permission.domain.{FacetContext, PermissionStatus}
import com.williamhill.permission.kafka.events.generic.{Header, InputEvent}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.{TableDrivenPropertyChecks, TableFor3}
import pureconfig.ConfigSource

class FacetContextParserSpec extends AnyFlatSpec with Matchers with TableDrivenPropertyChecks {

  behavior of "FacetContextParser"

  val scenarios: TableFor3[String, String, FacetContext] =
    Table(
      ("scenario", "path", "expected result"),
      (
        "Dormancy-dormant",
        "functional-tests/dormancy/in/dormant.json",
        FacetContext(
          Header(
            "c321d02c-9544-4aca-ba6e-6ad404ea32c9",
            who,
            "wh-eu-de",
            Instant.parse("2022-02-28T14:22:41.433Z"),
            Some("5848afcd-8020-11ec-a1d6-5057d25f6201"),
            None,
          ),
          Nil,
          playerId("U00004334"),
          universe("wh-eu-de"),
          "Dormancy",
          List(PermissionStatus("Dormant")),
          List(PermissionStatus("Active")),
        ),
      ),
      (
        "Prohibition-prohibited",
        "functional-tests/prohibition/in/prohibited.json",
        FacetContext(
          Header(
            "c321d02c-9544-4aca-ba6e-6ad404ea32c9",
            who,
            "wh-mga",
            Instant.parse("2022-01-31T13:22:41.454508Z"),
            Some("5848afcd-8020-11ec-a1d6-5057d25f6201"),
            None,
          ),
          Nil,
          playerId("U00004334"),
          universe("wh-mga"),
          "Prohibition",
          List(PermissionStatus("Prohibited")),
          List(PermissionStatus("Allowed")),
        ),
      ),
      (
        "Exclusion-indefinite",
        "functional-tests/excluded/in/indefinite.json",
        FacetContext(
          Header(
            "c321d02c-9544-4aca-ba6e-6ad404ea32c9",
            who,
            "wh-eu-dk",
            Instant.parse("2021-01-06T11:13:12.441799Z"),
            Some("445b4e38-84c1-11ec-a8a3-0242ac120002"),
            Some("48dac344-84c1-11ec-a8a3-0242ac120002"),
          ),
          Nil,
          playerId("U00004335"),
          universe("wh-eu-dk"),
          "excluded",
          List(PermissionStatus("indefinite", Some(Instant.parse("2022-02-11T00:00:00Z")))),
          Nil,
        ),
      ),
      (
        "Exclusion-permanent",
        "functional-tests/excluded/in/permanent.json",
        FacetContext(
          Header(
            "c321d02c-9544-4aca-ba6e-6ad404ea32c9",
            who,
            "wh-mga",
            Instant.parse("2021-01-06T11:13:12.441799Z"),
            Some("445b4e38-84c1-11ec-a8a3-0242ac120002"),
            Some("48dac344-84c1-11ec-a8a3-0242ac120002"),
          ),
          Nil,
          playerId("U00004336"),
          universe("wh-mga"),
          "excluded",
          List(PermissionStatus("permanent", Some(Instant.parse("2022-02-11T00:00:00Z")))),
          Nil,
        ),
      ),
      (
        "Exclusion-temporary",
        "functional-tests/excluded/in/temporary.json",
        FacetContext(
          Header(
            "c321d02c-9544-4aca-ba6e-6ad404ea32c9",
            who,
            "wh-mga",
            Instant.parse("2021-01-06T11:13:12.441799Z"),
            Some("445b4e38-84c1-11ec-a8a3-0242ac120002"),
            Some("48dac344-84c1-11ec-a8a3-0242ac120002"),
          ),
          Nil,
          playerId("U00005335"),
          universe("wh-mga"),
          "excluded",
          List(
            PermissionStatus("temporary", Some(Instant.parse("2021-01-06T11:13:11.993Z")), Some(Instant.parse("2021-12-06T11:13:11.993Z"))),
          ),
          Nil,
        ),
      ),
      (
        "Exclusion-timeout",
        "functional-tests/excluded/in/timeout.json",
        FacetContext(
          Header(
            "c321d02c-9544-4aca-ba6e-6ad404ea32c9",
            who,
            "wh-mga",
            Instant.parse("2021-01-06T11:13:12.441799Z"),
            Some("445b4e38-84c1-11ec-a8a3-0242ac120002"),
            Some("48dac344-84c1-11ec-a8a3-0242ac120002"),
          ),
          Nil,
          playerId("U00005335"),
          universe("wh-mga"),
          "excluded",
          List(
            PermissionStatus("timeout", Some(Instant.parse("2021-01-06T11:13:11.993Z")), Some(Instant.parse("2021-02-07T11:13:11.993Z"))),
          ),
          Nil,
        ),
      ),
    )

  val parser: FacetContextParser = new FacetContextParser(ConfigSource.resources("mappings.conf").loadOrThrow[MappingsConfig])

  forAll(scenarios) { case (scenario, path, expectedResult) =>
    it should s"convert $scenario input event to FacetContext" in {
      val inputEvent = FileReader.fromResources[InputEvent](path)
      parser.parse(inputEvent) shouldBe Right(expectedResult)
    }
  }
}
