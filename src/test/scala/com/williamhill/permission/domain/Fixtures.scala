package com.williamhill.permission.domain

import java.time.Instant

import com.williamhill.permission.kafka.events.generic.{Header, Who}

object Fixtures {

  val who: Who = Who("-1", "anonymous", "program", Some("100.77.100.167"))

  def header(universe: String): Header =
    Header(
      "c321d02c-9544-4aca-ba6e-6ad404ea32c9",
      who,
      universe,
      Instant.parse("2021-01-06T11:13:12.441799Z"),
      Some("445b4e38-84c1-11ec-a8a3-0242ac120002"),
      Some("48dac344-84c1-11ec-a8a3-0242ac120002"),
    )

  def universe(universeString: String): Universe = Universe(universeString).fold(e => throw e.toThrowable, identity)
}
