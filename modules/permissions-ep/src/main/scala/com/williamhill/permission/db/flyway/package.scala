package com.williamhill.permission.db

import com.williamhill.permission.config.DbConfig
import com.williamhill.permission.db.flyway.Flyway.Service
import org.flywaydb.core.Flyway as FlywayDb
import org.flywaydb.core.api.output.{CleanResult, MigrateResult}
import zio.blocking.{Blocking, effectBlocking}
import zio.{Has, RIO, RLayer, ZIO}

package object flyway {
  type Flyway = Has[Flyway.Service]

  object Flyway {
    type Service = FlywayDb

    val live: RLayer[Blocking & Has[DbConfig], Flyway] = flywayDb(true).toLayer
    val test: RLayer[Blocking & Has[DbConfig], Flyway] = flywayDb(false).toLayer
  }

  val migrateDb: RIO[Blocking & Flyway, MigrateResult] =
    ZIO.service[FlywayDb].flatMap(flyway => effectBlocking(flyway.migrate()))

  val cleanDb: RIO[Blocking & Flyway, CleanResult] =
    ZIO.service[FlywayDb].flatMap(flyway => effectBlocking(flyway.clean()))

  def flywayDb(cleanDisabled: Boolean): RIO[Blocking & Has[DbConfig], Service] = for {
    config <- ZIO.service[DbConfig]
    flyway <- effectBlocking(
      FlywayDb
        .configure()
        .cleanDisabled(cleanDisabled)
        .dataSource(config.url, config.username, config.password)
        .locations("classpath:db")
        .load(),
    )
  } yield flyway
}
