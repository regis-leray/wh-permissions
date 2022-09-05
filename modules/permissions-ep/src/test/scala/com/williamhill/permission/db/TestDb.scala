package com.williamhill.permission.db

import com.williamhill.permission.config.DbConfig
import com.williamhill.permission.db.flyway.{Flyway, cleanDb, migrateDb}
import com.williamhill.permission.db.postgres.Postgres
import doobie.syntax.connectionio.*
import doobie.util.Colors
import doobie.util.testing.*
import pureconfig.generic.semiauto.deriveReader
import pureconfig.{ConfigReader, ConfigSource}
import zio.ZIO.fail
import zio.blocking.Blocking
import zio.clock.Clock
import zio.interop.catz.*
import zio.test.Assertion.anything
import zio.test.{TestResult, assertM}
import zio.{Has, RIO, RLayer, ZIO, ZLayer, blocking}

trait TestDb {
  import TestDb.*

  val testDbConfigLayer: ZLayer[Blocking, Throwable, Has[DbConfig]] =
    blocking.blocking(ZIO.effect(ConfigSource.default.loadOrThrow[TestConfig].db)).toLayer

  val postgresFlywayLayer: RLayer[Clock & Blocking, Postgres & Flyway] =
    (ZLayer.identity[Clock & Blocking] ++ testDbConfigLayer) >>> (Flyway.test ++ Postgres.live)

  // Trigger the cleanup of database (remove tables & objects)
  val cleanMigrateDb: RIO[Blocking & Flyway, Unit] =
    (cleanDb *> migrateDb).unit

  def checkQuery[A: Analyzable](a: A): RIO[Clock & Blocking & Postgres, TestResult] =
    assertM(checkImpl(Analyzable.unpack(a)))(anything)
}

object TestDb extends TestDb {
  final private[permission] case class TestConfig(db: DbConfig)

  object TestConfig {
    implicit val reader: ConfigReader[TestConfig] = deriveReader
  }

  final private[permission] case class ErrorItems(errors: List[AnalysisReport.Item]) extends Exception

  private[permission] def checkImpl(args: AnalysisArgs): ZIO[Clock & Blocking & Postgres, Throwable, Unit] =
    ZIO.service[Postgres.Service].flatMap { xa =>
      analyze(args).transact(xa).flatMap { r =>
        fail(
          error = new Throwable(
            formatReport(args, r, Colors.Ansi).padLeft("  ").toString,
            ErrorItems(r.items.filter(_.error.isDefined)),
          ),
        ).unless(r.succeeded)
      }
    }
}
