package com.williamhill.permission.kafka

import zio.kafka.consumer.CommittableRecord
import com.williamhill.permission.application.AppError

import com.williamhill.permission.domain.FacetContext
import com.williamhill.permission.kafka.events.generic.{InputEvent, OutputEvent}
import com.williamhill.platform.kafka.consumer.Committable

object Record {

  type InputRecord = CommittableRecord[String, InputEvent]

  type FacetContextCommittable = Committable[AppError, FacetContext]
  type OutputCommittable       = Committable[AppError, OutputEvent]

}
