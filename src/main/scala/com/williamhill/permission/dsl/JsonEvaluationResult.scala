package com.williamhill.permission.dsl

import io.circe.Json

sealed trait JsonEvaluationResult {
  def asJson: Json
  def asJsonArr: Vector[Json]
}

object JsonEvaluationResult {
  case object Empty extends JsonEvaluationResult {
    override def asJson: Json            = Json.Null
    override def asJsonArr: Vector[Json] = Vector.empty
  }

  case class One(json: Json) extends JsonEvaluationResult {
    override def asJson: Json            = json
    override def asJsonArr: Vector[Json] = json.asArray.getOrElse(Vector(json))
  }

  case class Many(results: Vector[JsonEvaluationResult]) extends JsonEvaluationResult {
    override def asJson: Json            = Json.arr(results.flatMap(_.asJsonArr)*)
    override def asJsonArr: Vector[Json] = results.flatMap(_.asJsonArr)
  }
}
