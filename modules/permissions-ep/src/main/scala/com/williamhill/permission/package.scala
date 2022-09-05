package com.williamhill

import com.wh.permission.rule.dsl.Permission
import io.circe.{Codec, Decoder, Encoder}

package object permission {
  type State = Map[String, Set[Permission]]

  val emptyState: State = Map.empty

  implicit val stateCodec: Codec[State] = Codec.from(
    Decoder.decodeMap[String, Set[Permission]],
    Encoder.encodeMap[String, Set[Permission]],
  )
}
