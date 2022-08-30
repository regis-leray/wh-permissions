package com.williamhill.permission

import com.wh.permission.rule.dsl.errors.RuleError.InvalidAccountIdPath
import com.wh.permission.rule.dsl.{Permission, PermissionRule, Rules}
import com.williamhill.permission.kafka.events.generic.{InputEvent, InputHeader}
import com.williamhill.platform.event.common.{Header, PermissionDenial}
import com.williamhill.platform.event.permission.{Body, Data, NewValues, OutputAction, Event as OutputEvent}
import zio.{Has, IO, ULayer, ZLayer}

trait EventRuleProcessor {
  def handle(event: InputEvent): IO[InvalidAccountIdPath, List[(PermissionRule, OutputEvent)]]
}

object EventRuleProcessor {
  import com.williamhill.platform.event.common.Header as OutputHeader
  import io.circe.syntax.*

  val layer: ULayer[Has[EventRuleProcessor]] = ZLayer.succeed(EventRuleProcessor())

  def apply(): EventRuleProcessor = new EventRuleProcessor {
    override def handle(event: InputEvent): IO[InvalidAccountIdPath, List[(PermissionRule, OutputEvent)]] =
      Rules
        .run(event.asJson)(Rules.All)
        .map(_.map { case ((accountId, facet, permissions), rule) =>
          val denials = Permission.denied(permissions)
          val reason  = rule.name

          rule -> OutputEvent(
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
        })
  }

  private def fromInputHeader(inputHeader: InputHeader): OutputHeader =
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
