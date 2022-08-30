package com.williamhill.permission.domain

import com.williamhill.platform.event.common.Header
import com.williamhill.platform.event.common.Header.Who

object Fixtures {

  def who(universe: String): Who = Who(
    id = "-1",
    `type` = Header.Who.Type.Program,
    name = "anonymous",
    ip = Some("100.77.100.167"),
    sessionId = Some("445b4e38-84c1-11ec-a8a3-0242ac120002"),
    universe = Some(universe),
    allowedUniverses = Some(List(universe)),
  )

  def header(universeString: String): Header = {
    val id = "c321d02c-9544-4aca-ba6e-6ad404ea32c9"
    Header(
      id = id,
      traceContext = None,
      who = who(universeString),
      when = "2021-01-06T11:13:12.441799Z",
      universe = universeString,
    )
  }
}
