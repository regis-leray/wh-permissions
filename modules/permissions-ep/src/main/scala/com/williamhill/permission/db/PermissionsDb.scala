package com.williamhill.permission.db

import com.williamhill.permission.State
import doobie.{Get, Put, Query}
import doobie.ConnectionIO
import java.util.UUID
import doobie.postgres.implicits.*
import io.circe.syntax.*
import io.circe.parser.parse

object PermissionsDb {

  final case class PermissionRecord(id: UUID, playerId: String, current: State, old: State)

  implicit val StateGet: Get[State] =
    Get[String].temap(parse(_).flatMap(_.as[State]).left.map(_.getMessage))

  implicit val StatePut: Put[State] =
    Put[String].contramap(_.asJson.noSpaces)

  private[db] val upsertQuery: Query[PermissionRecord, UUID] = Query(
    """
      | INSERT INTO permissions (id, player_id, current_state, old_state, created_at, updated_at)
      | VALUES (?, ?, ?, ?, transaction_timestamp(), transaction_timestamp())
      | ON CONFLICT (id)
      | DO UPDATE SET
      |   current_state = EXCLUDED.current_state,
      |   old_state     = EXCLUDED.old_state,
      |   updated_at    = EXCLUDED.updated_at
      | RETURNING id
      |""".stripMargin,
  )

  val selectByPlayerIdQuery: Query[String, PermissionRecord] = Query(
    """
      | SELECT id, player_id, current_state, old_state
      | FROM permissions
      | WHERE player_id = ?
      |""".stripMargin,
  )

  val selectByIdQuery: Query[UUID, PermissionRecord] = Query(
    """
      | SELECT id, player_id, current_state, old_state
      | FROM permissions
      | WHERE id = ?
      |""".stripMargin,
  )

  def upsert(r: PermissionRecord): ConnectionIO[UUID] =
    upsertQuery.unique(r)

  def selectById(id: UUID): ConnectionIO[Option[PermissionRecord]] =
    selectByIdQuery.option(id)

  def selectByPlayerId(playerId: String): ConnectionIO[Option[PermissionRecord]] =
    selectByPlayerIdQuery.option(playerId)
}
