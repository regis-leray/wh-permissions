package com.williamhill.permission.application.config.dsl

import pureconfig.ConfigReader
import pureconfig.error.CannotConvert

sealed trait MappingValue[+T] {
  def optional: MappingValue[Option[T]]
}

object MappingValue {
  type Reader[A] = ConfigReader[MappingValue[A]]

  case class Path(path: String) extends MappingValue[Nothing] {
    def optional: Path = this
  }

  case class Const[+T](value: T) extends MappingValue[T] {
    def optional: Const[Option[T]] = Const(Some(value))
  }

  object Path {
    implicit val reader: ConfigReader[Path] = ConfigReader.stringConfigReader.emap {
      case s if s.startsWith("$.") => Right(Path(s.drop(2)))
      case s                       => Left(CannotConvert(s, "Path", s"Invalid json path format: $s"))
    }
  }

  object Const {
    implicit def reader[T: ConfigReader]: ConfigReader[Const[T]] = ConfigReader[T].map(Const(_))
  }

  implicit def reader[T: ConfigReader]: ConfigReader[MappingValue[T]] = Path.reader.orElse(Const.reader[T])
}
