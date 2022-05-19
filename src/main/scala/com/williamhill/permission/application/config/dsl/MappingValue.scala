package com.williamhill.permission.application.config.dsl

import pureconfig.ConfigReader
import pureconfig.error.CannotConvert

sealed trait MappingValue[+T]

object MappingValue {
  type Reader[A] = ConfigReader[MappingValue[A]]

  case class Path(path: String)     extends MappingValue[Nothing]
  case class Hardcoded[T](value: T) extends MappingValue[T]

  object Path {
    implicit val reader: ConfigReader[Path] = ConfigReader.stringConfigReader.emap {
      case s if s.startsWith("$.") => Right(Path(s.drop(2)))
      case s                       => Left(CannotConvert(s, "Path", s"Invalid json path format: $s"))
    }
  }

  object Hardcoded {
    implicit def reader[T: ConfigReader]: ConfigReader[Hardcoded[T]] = ConfigReader[T].map(Hardcoded(_))
  }

  implicit def reader[T: ConfigReader]: ConfigReader[MappingValue[T]] = Path.reader.orElse(Hardcoded.reader[T])
}
