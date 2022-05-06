package com.williamhill.permission.kafka

trait HasKey[T] {
  def key(t: T): String
}

object HasKey {
  def apply[T](implicit ev: HasKey[T]): HasKey[T] = ev

  implicit class SyntaxKey[T](private val t: T) extends AnyVal {
    def stringKey(implicit ev: HasKey[T]): String = ev.key(t)
  }
}
