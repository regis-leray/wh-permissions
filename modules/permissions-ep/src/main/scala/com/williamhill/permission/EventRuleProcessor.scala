package com.williamhill.permission

import com.wh.permission.rule.dsl.Rules
import com.williamhill.permission.db.PermissionsDb
import com.williamhill.permission.db.PermissionsDb.PermissionRecord
import com.williamhill.permission.db.postgres.Postgres
import com.williamhill.permission.db.syntax.*
import com.williamhill.permission.kafka.events.generic.{InputEvent, InputHeader}
import com.williamhill.platform.event.common.{Header, PermissionDenial}
import com.williamhill.platform.event.permission.{Body, Data, NewValues, OutputAction, Event as OutputEvent}
import io.circe.syntax.*
import zio.blocking.Blocking
import zio.clock.Clock
import zio.{Has, ULayer, ZIO, ZLayer}

import java.util.UUID

trait EventRuleProcessor {
  def handle(event: InputEvent): ZIO[Blocking & Clock & Postgres, Throwable, Option[OutputEvent]]
}

object EventRuleProcessor {
  val layer: ULayer[Has[EventRuleProcessor]] = ZLayer.succeed(EventRuleProcessor())

  def apply(): EventRuleProcessor = new EventRuleProcessor {
    override def handle(event: InputEvent): ZIO[Blocking & Clock & Postgres, Throwable, Option[OutputEvent]] = for {
      r <- Rules.run(event.asJson)(Rules.All).flatMap {
        case Nil => ZIO.none
        case ((accountId, facet, permissions), rule) :: Nil =>
          for {
            record <- PermissionsDb.selectByPlayerId(accountId).exec
            current = record.map(_.current).getOrElse(emptyState)

            // TODO replace facet by key string
            (newState, denials) = PermissionState.compute(current, facet.name -> permissions)
            _ <- PermissionsDb.upsert(PermissionRecord(record.fold(UUID.randomUUID())(_.id), accountId, newState, current)).exec
          } yield Some {
            val reason = rule.name

            OutputEvent(
              fromInputHeader(event.header),
              Body(
                facet.name.toLowerCase,
                NewValues(
                  id = accountId,
                  universe = event.header.universe.toLowerCase,
                  data = Data(
                    permissionDenials = denials.map(p => (p.name, Vector(PermissionDenial(reason, Some(reason), None)))).toMap,
                    actions = Vector(
                      OutputAction(
                        name = rule.name,
                        relatesToPermissions = denials.map(_.name).toList,
                        `type` = "notification",
                        deadline = None,
                        reasonCode = reason,
                      ),
                    ),
                  ),
                ),
              ),
            )
          }
        case others =>
          ZIO.fail(
            new Exception(
              s"Error in permissions rules definition ! Duplicate rules ${others.map(_._2.name)} applies for same event : ${event.asJson.noSpaces}",
            ),
          )
      }

    } yield r
  }

  private def fromInputHeader(inputHeader: InputHeader): Header =
    Header(
      id = inputHeader.id,
      traceContext = None,
      universe = inputHeader.universe,
      when = inputHeader.when.toString,
      who = Header.Who(
        id = inputHeader.who.id,
        name = inputHeader.who.name,
        `type` = inputHeader.who.`type` match {
          case "customer" => Header.Who.Type.Customer
          case "staff"    => Header.Who.Type.Staff
          case "program"  => Header.Who.Type.Program
        },
        sessionId = inputHeader.sessionId,
        ip = inputHeader.who.ip,
        universe = Some(inputHeader.universe),
        // TODO change to allow germany and mga like in Processor (take list from config)
        allowedUniverses = Some(List(inputHeader.universe)),
      ),
    )
}
