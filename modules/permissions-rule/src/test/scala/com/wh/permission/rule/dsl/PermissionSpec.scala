package com.wh.permission.rule.dsl

import cats.data.NonEmptySet
import Permission.{denied, deny, denyAll, grant, grantAll, granted, order}
import zio.random.Random
import zio.test.environment.TestEnvironment
import zio.test.{DefaultRunnableSpec, Gen, ZSpec}
import zio.test.*
import zio.test.Assertion.*

import scala.collection.immutable.SortedSet
import cats.instances.order.*

object PermissionSpec extends DefaultRunnableSpec {
  def genNonEmptySet[A: Ordering](l: NonEmptySet[A]): Gen[Random & Sized, NonEmptySet[A]] =
    Gen.setOf1(Gen.fromIterable(l.toSortedSet)).map(s => NonEmptySet.fromSetUnsafe(SortedSet.from(s)))

  override def spec: ZSpec[TestEnvironment, Any] = suite("Permissions Spec")(deniedSpec + grantedSpec)

  private def deniedSpec = {
    testM("deny should return some denied permissions") {
      check(genNonEmptySet(Permission.All)) { perms =>
        val denyPerms = deny(perms)
        assert(denied(denyPerms))(equalTo(perms.toSortedSet))
      }
    } + testM("deny should return some granted permissions") {
      check(genNonEmptySet(Permission.All)) { perms =>
        val grants    = perms.diff(Permission.All)
        val denyPerms = deny(perms)
        assert(granted(denyPerms))(equalTo(grants))
      }
    } + test("deny all permissions should return all denied permissions & empty granted permissions") {
      val denyPerms = denyAll
      assert(denied(denyPerms))(equalTo(Permission.All.toSortedSet)) && assert(granted(denyPerms))(isEmpty)
    }
  }

  private def grantedSpec =
    testM("grant should return some grant permissions") {
      check(genNonEmptySet(Permission.All)) { perms =>
        val grantPerms = grant(perms)
        assert(granted(grantPerms))(equalTo(perms.toSortedSet))
      }
    } + testM("grant should return some deny permissions") {
      check(genNonEmptySet(Permission.All)) { perms =>
        val denies     = perms.diff(Permission.All)
        val grantPerms = grant(perms)

        assert(denied(grantPerms))(equalTo(denies))
      }
    } + test("grant all permissions should return all granted permissions & empty denied permissions") {
      val grantPerms = grantAll
      assert(granted(grantPerms))(equalTo(Permission.All.toSortedSet)) && assert(denied(grantPerms))(isEmpty)
    }
}
