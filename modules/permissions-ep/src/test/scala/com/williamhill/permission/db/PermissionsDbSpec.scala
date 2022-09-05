package com.williamhill.permission.db

import com.williamhill.permission.db.PermissionsDb.{selectByIdQuery, selectByPlayerIdQuery, upsertQuery}
import zio.test.{DefaultRunnableSpec, TestAspect, TestFailure, ZSpec}

object PermissionsDbSpec extends DefaultRunnableSpec with TestDb {

  override def spec: ZSpec[Environment, Failure] =
    suite("Permissions Db Spec")(queries)
      .@@(TestAspect.beforeAll(cleanMigrateDb))
      .provideSomeLayerShared[Environment](postgresFlywayLayer.mapError(TestFailure.fail))

  val queries = testM("upsertQuery") {
    checkQuery(upsertQuery)
  } +
    testM("selectByIdQuery") {
      checkQuery(selectByIdQuery)
    } +
    testM("selectByPlayerIdQuery") {
      checkQuery(selectByPlayerIdQuery)
    }

}
