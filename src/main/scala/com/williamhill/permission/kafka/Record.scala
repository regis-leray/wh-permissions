package com.williamhill.permission.kafka

import com.williamhill.permission.application.AppError
import com.williamhill.permission.kafka.events.generic.{InputEvent, OutputEvent}
import com.williamhill.platform.kafka.consumer.Committable
import zio.kafka.consumer.CommittableRecord

object Record {

  type InputRecord       = CommittableRecord[String, InputEvent]
  type OutputCommittable = Committable[AppError, OutputEvent]

}
