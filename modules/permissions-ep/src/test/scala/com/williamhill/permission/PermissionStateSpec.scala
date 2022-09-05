package com.williamhill.permission

import cats.data.NonEmptySet
import cats.instances.order.*
import com.wh.permission.rule.dsl.Permission
import com.wh.permission.rule.dsl.Permission.*
import zio.random.Random
import zio.test.environment.TestEnvironment
import zio.test.{DefaultRunnableSpec, ZSpec}
import zio.test.*
import zio.test.Assertion.*
import scala.collection.immutable.SortedSet

object PermissionStateSpec extends DefaultRunnableSpec {

  def genNonEmptySet[A: Ordering](l: NonEmptySet[A]): Gen[Random & Sized, NonEmptySet[A]] =
    Gen.setOf1(Gen.fromIterable(l.toSortedSet)).map(s => NonEmptySet.fromSetUnsafe(SortedSet.from(s)))

  override def spec: ZSpec[TestEnvironment, Any] = suite("PermissionStateSpec")(spec1)

  def spec1 =
    testM("compute grant with empty state") {
      check(genNonEmptySet(Permission.All)) { permissions =>
        val (newState, perms) = PermissionState.compute(Map.empty, ("key1", grant(permissions)))

        assert(newState)(isEmpty)
        assert(perms)(isEmpty)
      }
    } + testM("compute deny with empty state") {
      check(genNonEmptySet(Permission.All)) { permissions =>
        val (newState, perms) = PermissionState.compute(Map.empty, ("key1", deny(permissions)))

        assert(newState)(equalTo(Map(("key1", permissions.toSortedSet))))
        assert(perms)(equalTo(permissions.toSortedSet))
      }
    } + testM("compute with grantAll ") {
      check(genNonEmptySet(Permission.All)) { permissions =>
        val current           = Map("key1" -> permissions.toSortedSet)
        val (newState, perms) = PermissionState.compute(current, ("key1", grantAll))

        assert(newState)(isEmpty)
        assert(perms)(isEmpty)
      }
    } + testM("compute with denyAll ") {
      check(genNonEmptySet(Permission.All)) { permissions =>
        val current           = Map("key1" -> permissions.toSortedSet)
        val (newState, perms) = PermissionState.compute(current, ("key1", denyAll))

        assert(newState)(equalTo(Map(("key1", Permission.All.toSortedSet))))
        assert(perms)(equalTo(Permission.All.toSortedSet))
      }
    } + test("compute with grant ") {
      val current           = Map("key1" -> Set[Permission](Permission.CanLoginWithPassword))
      val (newState, perms) = PermissionState.compute(current, ("key1", grant(CanLoginWithPassword)))

      assert(newState)(isEmpty)
      assert(perms)(isEmpty)
    } + test("compute with deny ") {
      val current           = Map("key1" -> Set[Permission](CanLoginWithPassword))
      val (newState, perms) = PermissionState.compute(current, ("key1", deny(CanLoginWithPassword, CanBet)))

      assert(newState)(equalTo(Map(("key1", Set[Permission](CanLoginWithPassword, CanBet)))))
      assert(perms)(equalTo(Set[Permission](CanLoginWithPassword, CanBet)))
    }
}
