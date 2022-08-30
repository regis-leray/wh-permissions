package com.wh.permission.rule.dsl

import io.circe.optics.JsonPath

import java.time.{Instant, LocalDate}

/** Defines the type of data that a schema can use to define a field.
  */
sealed trait FieldType[+A]

object FieldType {

  def apply[A: FieldType]: FieldType[A] = implicitly[FieldType[A]]

  sealed trait NotNull[+A]      extends FieldType[A]
  implicit case object TString  extends NotNull[String]
  implicit case object TInt     extends NotNull[Int]
  implicit case object TBool    extends NotNull[Boolean]
  implicit case object TDate    extends NotNull[LocalDate]
  implicit case object TInstant extends NotNull[Instant]

  object NotNull {
    def apply[A: NotNull]: NotNull[A] = implicitly[NotNull[A]]
    def unapply[A](nna: NotNull[A]): Some[NotNull[A]] =
      Some(nna)
  }

  final case class Nullable[A: NotNull]() extends FieldType[Option[A]] {
    val fieldType: NotNull[A] = implicitly[NotNull[A]]
  }
  object AnyNullable {
    def unapply[A](x: Nullable[A]): Some[NotNull[A]] = Some(x.fieldType)
  }
  implicit def some[A: NotNull]: Nullable[A] = Nullable[A]()

  final case class Many[A: NotNull]() extends FieldType[List[A]] {
    val fieldType: NotNull[A] = implicitly[NotNull[A]]
  }
  object AnyMany {
    def unapply[A](x: Many[A]): Some[NotNull[A]] = Some(x.fieldType)
  }

  implicit def many[A: NotNull]: Many[A] = Many[A]()
}

final case class JsonRecord[A: FieldType](path: JsonPath) {
  val fieldType: FieldType[A] = implicitly[FieldType[A]]

  def optional(implicit ev: FieldType.NotNull[A]): JsonRecord[Option[A]] =
    JsonRecord(path)

  def many(implicit ev: FieldType.Many[A]): JsonRecord[List[A]] =
    JsonRecord[List[A]](path)(ev)
}
