package com.williamhill.permission.kafka

import com.williamhill.permission.application.AppError
import com.williamhill.permission.kafka.events.generic.{InputEvent, OutputEvent}
import com.williamhill.platform.kafka.consumer.Committable
import zio.kafka.consumer.CommittableRecord

object Record {

  type StringRecord      = CommittableRecord[String, String]
  type InputRecord       = CommittableRecord[String, InputEvent]
  type InputCommittable  = Committable[AppError, InputEvent] // str
  type OutputCommittable = Committable[AppError, OutputEvent]

//  final case class InputCommittable(committable: Committable[AppError, InputEvent], topic: String)
}
