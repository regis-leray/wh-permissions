package com.williamhill.permission

import io.circe.Decoder
import io.circe.parser.*

import scala.io.Source

object FileReader {

  def fromResources[A: Decoder](path: String): A = {
    val source = Source.fromResource(path)
    try {
      val jsonString = source.getLines().toList.mkString
      decode[A](jsonString).fold(e => throw new RuntimeException(e.getMessage), identity)
    } finally source.close()
  }
}
