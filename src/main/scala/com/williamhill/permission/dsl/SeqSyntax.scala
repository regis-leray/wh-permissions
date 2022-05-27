package com.williamhill.permission.dsl

import scala.annotation.tailrec

object SeqSyntax {

  implicit class SeqExt[T](seq: Seq[T]) {

    def collectSome[U](f: T => Option[U]): Option[U] = collectSomeRec(seq, f)

    def traverseSome[U, E](f: T => Either[E, Option[U]]): Either[E, Option[U]] = traverseSomeRec(seq, f)

    @tailrec
    final private def collectSomeRec[U](seq: Seq[T], f: T => Option[U]): Option[U] = seq match {
      case empty if empty.isEmpty => None
      case nonEmpty =>
        f(nonEmpty.head) match {
          case None   => collectSomeRec(seq.tail, f)
          case result => result
        }
    }

    @tailrec
    final private def traverseSomeRec[U, E](seq: Seq[T], f: T => Either[E, Option[U]]): Either[E, Option[U]] = seq match {
      case empty if empty.isEmpty => Right(None)
      case nonEmpty =>
        f(nonEmpty.head) match {
          case Right(None) => traverseSomeRec(nonEmpty.tail, f)
          case result      => result
        }
    }
  }

}
