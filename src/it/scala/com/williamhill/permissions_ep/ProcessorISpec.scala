package com.williamhill.permissions_ep

import java.time.Instant

import zio.*
import zio.blocking.Blocking
import zio.clock.Clock
import zio.duration.durationInt
import zio.kafka.consumer.{Consumer, ConsumerSettings}

import com.williamhill.permission.Processor
import com.williamhill.permission.application.Env.Processor
import com.williamhill.permission.kafka.events.generic.{Header, InputEvent, Who}
import io.circe.parser.*
import org.scalatest.funsuite.AnyFunSuite
import monocle.syntax.all.*
import zio.kafka.serde.Serde as ZioSerde

/** When using zio-test for this it fails.
  * It hangs after the processor subscribes to the input topic, with
  *
  * ```
  * Warning: A test is using time, but is not advancing the test clock, which may result in the test hanging. Use TestClock.adjust to manually advance the time.
  * Test Processor - should process a exclusion message has taken more than 1 m to execute. If this is not expected, consider using TestAspect.timeout to timeout runaway tests for faster diagnostics.
  * ```
  *
  * Have to use another test framework like scalatest
  *
  * TODO: I think there is an issue with offset-retrieval configuration value.For the moment I am hard-coding the value in TestEnv, but needs to be resolved
  */
class ProcessorISpec extends AnyFunSuite {

  test("should process a exclusion message") {
    val testInput = InputEvent(
      Header(
        id = "1",
        who = Who(id = "-1", name = "anonymous", `type` = "program", ip = Some("100.77.100.167")),
        universe = "wh-eu-de",
        when = Instant.parse("2022-02-28T14:22:41.433Z"),
        sessionId = Some("5848afcd-8020-11ec-a1d6-5057d25f6201"),
        traceId = None,
      ),
      parse("""{
              |  "type": "Dormancy",
              |  "newValues": {
              |   "id": "U00004334",
              |   "universe": "wh-eu-de",
              |    "data": {
              |        "status": "Dormant"
              |     }
              |    }
              |}
              """.stripMargin).toOption.get,
    )
    val program: ZIO[
      Clock
        & Blocking
        & Has[TestOutputEventConsumer]
        & Processor
        & Has[ConsumerSettings]
        & Has[TestInputEventProducer]
        & Has[ZioSerde[Any, InputEvent]],
      Object,
      Unit,
    ] = for {
      event             <- ZIO.succeed(testInput)
      exclusionProducer <- ZIO.service[TestInputEventProducer]
      _                 <- exclusionProducer.publishEvent(event)
      _                 <- ZIO.service[Consumer]
      _                 <- exclusionProducer.publishEvent(event.focus(_.header.id).replace("2"))
      fiber             <- Processor.run.fork
      _                 <- fiber.interrupt.delay(4.seconds)
      facetConsumer     <- ZIO.service[TestOutputEventConsumer]
      fiber2            <- facetConsumer.eventCount.fork
      _                 <- fiber2.interrupt.delay(4.seconds)
    } yield ()

    Runtime.default.unsafeRun(program.provideCustomLayer(ZEnv.live >>> TestEnv.testLayer))

    assert(TestOutputEventConsumer.size == 2)

  }

}
