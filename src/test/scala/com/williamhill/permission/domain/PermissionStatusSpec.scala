package com.williamhill.permission.domain

import com.williamhill.permission.FileReader
import com.williamhill.permission.kafka.events.generic.InputEvent
import io.circe.Json
import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.{TableDrivenPropertyChecks, TableFor4}

import java.time.Instant

class PermissionStatusSpec extends AnyFlatSpec with Matchers with TableDrivenPropertyChecks with EitherValues {

  behavior of "PermissionStatus#fromJsonBody"

  val scenarios: TableFor4[String, String, String, PermissionStatus] =
    Table(
      ("scenario", "path", "even type", "expected result"),
      ("Dormancy-dormant", "input-events/dormancy/dormant.json", "Dormancy", PermissionStatus("Dormant")),
      ("Prohibition-prohibited", "input-events/prohibition/prohibited.json", "Prohibition", PermissionStatus("Prohibited")),
      (
        "Exclusion-indefinite",
        "input-events/excluded/indefinite.json",
        "excluded",
        PermissionStatus("indefinite", Some(Instant.parse("2022-02-11T00:00:00Z"))),
      ),
      (
        "Exclusion-permanent",
        "input-events/excluded/permanent.json",
        "excluded",
        PermissionStatus("permanent", Some(Instant.parse("2022-02-11T00:00:00Z"))),
      ),
      (
        "Exclusion-temporary",
        "input-events/excluded/temporary.json",
        "excluded",
        PermissionStatus("temporary", Some(Instant.parse("2021-01-06T11:13:11.993Z")), Some(Instant.parse("2021-12-06T11:13:11.993Z"))),
      ),
      (
        "Exclusion-timeout",
        "input-events/excluded/timeout.json",
        "excluded",
        PermissionStatus("timeout", Some(Instant.parse("2021-01-06T11:13:11.993Z")), Some(Instant.parse("2021-02-07T11:13:11.993Z"))),
      ),
    )

  forAll(scenarios) { case (scenario, path, eventType, expectedResult) =>
    it should s"extract permission context from json body for $scenario" in {
      FileReader
        .fromResources[InputEvent](path)
        .body
        .hcursor
        .downField("newValues")
        .as[Json]
        .flatMap(PermissionStatus.fromJsonBodyValues(eventType))
        .value shouldBe expectedResult
    }
  }
}
