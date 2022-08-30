package com.williamhill.permission.domain
import enumeratum.*

case class Subject(id: String, universe: Universe, `type`: SubjectType, sessionId: Option[String])

sealed trait SubjectType extends EnumEntry

object SubjectType extends Enum[SubjectType] {

  val values: IndexedSeq[SubjectType] = findValues
  case object Customer extends SubjectType
  case object Staff    extends SubjectType
  case object Program  extends SubjectType

}
