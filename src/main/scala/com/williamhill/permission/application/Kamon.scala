package com.williamhill.permission.application

import zio.{TaskManaged, ZIO, ZManaged}

object Kamon {
  val asResource: TaskManaged[kamon.Kamon.type] =
    ZManaged.make(ZIO.effect { kamon.Kamon.init(); kamon.Kamon })(kamon => ZIO.fromFuture(_ => kamon.stop()).orDie)
}
