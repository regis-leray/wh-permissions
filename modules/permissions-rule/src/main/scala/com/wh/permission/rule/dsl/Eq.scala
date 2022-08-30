package com.wh.permission.rule.dsl

import java.time.{Instant, LocalDate}

sealed trait Eq[T] {
  def eqv(t1: T, t2: T): Boolean
}

object Eq {
  def apply[T](implicit ev: Eq[T]): Eq[T] = ev

  def instance[T](f: (T, T) => Boolean): Eq[T] = new Eq[T] {
    override def eqv(t1: T, t2: T): Boolean = f(t1, t2)
  }

  implicit val intEq: Eq[Int] = Eq.instance(_ == _)

  implicit val listIntEq: Eq[List[Int]] = Eq.instance(_ == _)

  implicit val stringEq: Eq[String] = Eq.instance(_ == _)

  implicit val listStringEq: Eq[List[String]] = Eq.instance(_ == _)

  implicit val boolEq: Eq[Boolean] = Eq.instance(_ == _)

  implicit val listBoolEq: Eq[List[Boolean]] = Eq.instance(_ == _)

  implicit val localDateEq: Eq[LocalDate] = Eq.instance(_ == _)

  implicit val listLocalDateEq: Eq[List[LocalDate]] = Eq.instance(_ == _)

  implicit val instantEq: Eq[Instant] = Eq.instance(_ == _)

  implicit val listInstantEq: Eq[List[Instant]] = Eq.instance(_ == _)
}
