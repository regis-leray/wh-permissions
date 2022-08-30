package com.williamhill.permission

import zio.RIO
import zio.blocking.Blocking
import zio.clock.Clock

package object endpoints {
  type Effect[A] = RIO[Clock & Blocking, A]
}
