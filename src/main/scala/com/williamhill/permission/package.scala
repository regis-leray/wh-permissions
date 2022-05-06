package com.williamhill

import zio.{Has, ZEnv}
import zio.kafka.consumer.Consumer
import zio.kafka.producer.Producer

import com.github.mlangc.slf4zio.api.Logging
import com.williamhill.permission.kafka.EventPublisher
import org.http4s.server.Server

package object permission {

  type Env =
    Has[Consumer] & Has[Producer] & Has[Server] & Has[Processor.Config] & ZEnv & Logging & Has[EventPublisher]

  type ProcessorEnv = ZEnv & Logging & Has[Processor.Config] & Has[EventPublisher]
}
