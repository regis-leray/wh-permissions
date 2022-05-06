package com.williamhill.permission.application

import com.typesafe.scalalogging.Logger

package object logging {

  implicit class RichLogger(val logger: Logger) {

    def error(msg: String, err: AppError): Unit = error(err, msg)

    def warn(msg: String, err: AppError): Unit = warn(err, msg)

    def error(err: AppError, msg: String = ""): Unit =
      err.cause match {
        case None            => logger.warn(msg + " - " + err.logMessage)
        case Some(throwable) => logger.error(msg + " - " + err.logMessage, throwable)
      }

    def warn(err: AppError, msg: String = ""): Unit =
      logger.warn(msg + " - " + err.logMessage)
  }
}
