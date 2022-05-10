package com.williamhill.permission

import com.williamhill.permission.domain.Fixtures.*
import com.williamhill.permission.domain.{FacetContext, PermissionStatus}
import com.williamhill.permission.kafka.Record.InputRecord
import com.williamhill.permission.kafka.events.generic.{Header, InputEvent}
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.{TableDrivenPropertyChecks, TableFor3}
import zio.ZIO
import zio.kafka.consumer.CommittableRecord

import java.time.Instant

class InputParserSpec extends AnyFlatSpec with Matchers with TableDrivenPropertyChecks {

  behavior of "InputParser"

  val scenarios: TableFor3[String, String, FacetContext] =
    Table(
      ("scenario", "path", "expected result"),
      (
        "Dormancy-dormant",
        "input-events/dormancy/dormant.json",
        FacetContext(
          Header(
            "c321d02c-9544-4aca-ba6e-6ad404ea32c9",
            who,
            "wh-us-ny",
            Instant.parse("2022-02-28T14:22:41.433Z"),
            Some("5848afcd-8020-11ec-a1d6-5057d25f6201"),
            None,
          ),
          Nil,
          playerId("U00004334"),
          universe("wh-us-ny"),
          "Dormancy",
          PermissionStatus("Dormant"),
          Some(PermissionStatus("Active")),
        ),
      ),
      (
        "Prohibition-prohibited",
        "input-events/prohibition/prohibited.json",
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
          PermissionStatus("Prohibited"),
          Some(PermissionStatus("Allowed")),
        ),
      ),
      (
        "Exclusion-indefinite",
        "input-events/excluded/indefinite.json",
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
          PermissionStatus("indefinite", Some(Instant.parse("2022-02-11T00:00:00Z"))),
          None,
        ),
      ),
      (
        "Exclusion-permanent",
        "input-events/excluded/permanent.json",
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
          PermissionStatus("permanent", Some(Instant.parse("2022-02-11T00:00:00Z"))),
          None,
        ),
      ),
      (
        "Exclusion-temporary",
        "input-events/excluded/temporary.json",
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
          PermissionStatus("temporary", Some(Instant.parse("2021-01-06T11:13:11.993Z")), Some(Instant.parse("2021-12-06T11:13:11.993Z"))),
          None,
        ),
      ),
      (
        "Exclusion-timeout",
        "input-events/excluded/timeout.json",
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
          PermissionStatus("timeout", Some(Instant.parse("2021-01-06T11:13:11.993Z")), Some(Instant.parse("2021-02-07T11:13:11.993Z"))),
          None,
        ),
      ),
    )

  forAll(scenarios) { case (scenario, path, expectedResult) =>
    it should s"convert $scenario input event to FacetContext" in {
      val inputEvent = FileReader.fromResources[InputEvent](path)

      InputParser.parse(committableRecord(inputEvent)) shouldBe Some(expectedResult)
    }
  }

  def committableRecord(inputEvent: InputEvent): InputRecord =
    CommittableRecord(
      new ConsumerRecord[String, InputEvent]("topic", 1, 1, "key", inputEvent),
      _ => ZIO.unit,
      None,
    )
}
