package com.wh.permission.rule

import com.wh.permission.rule.dsl.FieldType.NotNull
import io.circe.optics.JsonPath

import java.time.{Instant, LocalDate}

package object dsl {

  val $ = io.circe.optics.JsonPath.root

  /** Creates an record typed as an list of A
    *
    * @param path the json path of the Record array
    */
  def list[A: NotNull](path: JsonPath): JsonRecord[List[A]] = JsonRecord[List[A]](path)

  /** Creates an record typed as an integer
    *
    * @param path the json path of the Record
    */
  def int(path: JsonPath): JsonRecord[Int] = JsonRecord[Int](path)

  /** Creates an record typed as a list of int
    *
    * @param path the json path of the Record array
    */
  def ints(path: JsonPath): JsonRecord[List[Int]] = list[Int](path)

  /** Creates an record typed as a string
    *
    * @param path the json path of the Record
    */
  def string(path: JsonPath): JsonRecord[String] = JsonRecord[String](path)

  /** Creates an record typed as a List of String
    *
    * @param path the json path of the Record array
    */
  def strings(path: JsonPath): JsonRecord[List[String]] = list[String](path)

  /** Creates an record typed as a boolean
    *
    * @param path the json path of the Record
    */
  def boolean(path: JsonPath): JsonRecord[Boolean] = JsonRecord[Boolean](path)

  /** Creates an record typed as a list of boolean
    *
    * @param path the json path of the Record array
    */
  def booleans(path: JsonPath): JsonRecord[List[Boolean]] = list[Boolean](path)

  /** Creates an record typed as a date
    *
    * @param path the json path of the Record
    */
  def date(path: JsonPath): JsonRecord[LocalDate] = JsonRecord[LocalDate](path)

  /** Creates an record typed as a list of date
    *
    * @param path the json path of the Record array
    */
  def dates(path: JsonPath): JsonRecord[List[LocalDate]] = list[LocalDate](path)

  /** Creates an record typed as an instant
    *
    * @param path the json path of the Record
    */
  def instant(path: JsonPath): JsonRecord[Instant] = JsonRecord[Instant](path)

  /** Creates an record typed as a list of instant
    *
    * @param path the json path of the Record array
    */
  def instants(path: JsonPath): JsonRecord[List[Instant]] = JsonRecord[List[Instant]](path)
}
