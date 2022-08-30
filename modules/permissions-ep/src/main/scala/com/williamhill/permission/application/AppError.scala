package com.williamhill.permission.application

import cats.syntax.either.*

//TODO check if we want error status
case class AppError(message: String, cause: Option[Throwable] = None) {
  def logMessage: String     = Either.catchNonFatal(message.replaceAll("\n", " ")).getOrElse("")
  def toThrowable: Throwable = cause.fold(new RuntimeException(message))(th => new RuntimeException(message, th))
}

object AppError {
  def fromMessage(message: String): AppError                           = AppError(message, None)
  def fromThrowable(throwable: Throwable): AppError                    = AppError(throwable.getMessage, Some(throwable))
  def fromDecodingFailure(failure: io.circe.DecodingFailure): AppError = AppError(failure.getMessage, Option(failure))
}
