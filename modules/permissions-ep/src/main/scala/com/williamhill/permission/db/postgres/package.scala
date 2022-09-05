package com.williamhill.permission.db

import com.williamhill.permission.config.DbConfig
import com.williamhill.permission.db.postgres.Postgres.{Effect, Service}
import doobie.{ExecutionContexts, Transactor}
import doobie.hikari.HikariTransactor
import zio.blocking.Blocking
import zio.clock.Clock
import zio.{Has, RIO, RManaged, ZIO, ZLayer, ZManaged}
import zio.interop.catz.*

package object postgres {
  type Postgres = Has[Service]

  object Postgres {
    type Service    = Transactor[Effect]
    type Effect[+A] = RIO[Clock & Blocking, A]

    val live: ZLayer[Has[DbConfig] & Clock & Blocking, Throwable, Postgres] =
      transactor.toLayer
  }

  // Resource yielding a transactor configured with a bounded connect EC and an unbounded
  // transaction EC. Everything will be closed and shut down cleanly after use.
  val transactor: RManaged[Clock & Blocking & Has[DbConfig], Service] =
    for {
      config <- ZIO.service[DbConfig].toManaged_
      ec     <- ExecutionContexts.fixedThreadPool[Effect](32).toManagedZIO
      transactor <- HikariTransactor
        .newHikariTransactor[Effect](
          driverClassName = "org.postgresql.Driver",
          url = config.url,
          user = config.username,
          pass = config.password,
          connectEC = ec,
        )
        .toManagedZIO
      /* The auto-commit flag is set to true by default. The problem with this
         setting is that it implies that every modifications will be committed
         by default. This prevents rolling-back a set of instructions that is
         supposed to be performed as a single unit. Secondly, changing the
         auto-commit at each step also implies a performance issue. Finally,
         Doobie is expecting the auto-commit flag to be set to false but
         Hikari keeps this true by default so we adjust it for the sake of Doobie.
         More info here: https://developpaper.com/question/should-autocommit-of-datasource-connection-pool-be-set-to-false/
       */
      _ <- ZManaged.succeed(transactor.kernel.setAutoCommit(false))
    } yield transactor
}
