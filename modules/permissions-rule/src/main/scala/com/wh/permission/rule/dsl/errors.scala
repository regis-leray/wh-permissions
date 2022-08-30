package com.wh.permission.rule.dsl

import io.circe.Json
import io.circe.optics.JsonPath

// TODO pretty printing json path
object errors {
  sealed abstract class RuleError(val message: String) extends Exception(message)

  object RuleError {
    final case class InvalidAccountIdPath(json: Json, rule: PermissionRule)
        extends RuleError(
          s"Error in permission rule definition. Account id json path is invalid ${rule.accountId} with json: ${json.noSpaces}",
        )
  }

  sealed abstract class ParsingError(message: String) extends RuleError(message)

  object ParsingError {
    final case class MissingField(path: JsonPath, value: Json) extends ParsingError(s"Critical parsing error, missing json field: $path")

    final case class TypeMismatch(path: JsonPath, expectedType: String, value: Json)
        extends ParsingError(
          s"Critical parsing error, type mismatch for field name $path, expected $expectedType, json was ${value.noSpaces}",
        )
  }
}
