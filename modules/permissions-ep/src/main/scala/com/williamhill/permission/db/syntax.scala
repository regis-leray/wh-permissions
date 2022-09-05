package com.williamhill.permission.db

import com.williamhill.permission.db.postgres.Postgres
import doobie.ConnectionIO
import zio.blocking.Blocking
import zio.clock.Clock
import zio.{RIO, ZIO}
import zio.interop.catz.*
import doobie.implicits.*

object syntax {
  implicit class DBSyntax[A](val self: ConnectionIO[A]) extends AnyVal {
    def exec: RIO[Clock & Blocking & Postgres, A] =
      ZIO.service[Postgres.Service].flatMap(self.transact(_))
  }
}
