package com.williamhill.permission

import cats.data.NonEmptyList
import com.whbettingengine.kafka.serialization.TopicNameStrategy
import com.whbettingengine.kafka.serialization.json.JsonSerializerConfig
import com.williamhill.permission.application.Env
import com.williamhill.permission.config.AppConfig
import com.williamhill.permission.db.TestDb
import com.williamhill.permission.kafka.events.generic.OutputEvent.OutputEvent
import com.williamhill.permission.kafka.events.generic.{InputEvent, InputHeader, Who}
import com.williamhill.platform.kafka.JsonSerialization
import io.circe.parser.*
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.{ProducerRecord, RecordMetadata}
import zio.*
import zio.blocking.Blocking
import zio.clock.Clock
import zio.duration.durationInt
import zio.kafka.consumer.{Consumer, ConsumerSettings, Subscription}
import zio.kafka.producer.{Producer, ProducerSettings}
import zio.kafka.serde.Serde as ZSerde
import zio.magic.*
import zio.random.Random
import zio.stream.ZStream
import zio.test.*
import zio.test.Assertion.*

import java.time.Instant
import java.util.UUID

object ProcessorSpec extends DefaultRunnableSpec with TestDb {

  private val event = InputEvent(
    InputHeader(
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
        |        "status": "dormant"
        |     }
        |    }
        |}
              """.stripMargin).toOption.get,
  )

  val appConfig: ZLayer[Blocking & Random, Throwable, Has[AppConfig]] = ZIO
    .service[Random.Service]
    .flatMap(_.nextLongBetween(1, 1000000).map("Test_" + _))
    .toLayer
    .flatMap { randomTopic =>
      AppConfig.live.update[AppConfig] { config =>
        val procConf        = config.processorSettings
        val newOutputEvents = procConf.outputEvents.copy(NonEmptyList.one(randomTopic.get))
        config.copy(processorSettings = procConf.copy(outputEvents = newOutputEvents))
      }
    }

  override def spec: ZSpec[Environment, Failure] = {
    suite("ProcessorSpec")(processSpec)
      .@@(TestAspect.beforeAll(cleanMigrateDb))
      .injectShared(
        Blocking.live ++ Random.live ++ Clock.live,
        postgresFlywayLayer ++ Env.core,
        appConfig ++ appConfig.map(c => Has(c.get.processorSettings)) ++ testDbConfigLayer,
      )
      .mapError(TestFailure.fail)
      .@@(TestAspect.timeout(15.seconds))
  }

  private val processSpec = testM("should process a dormant message") {
    for {
      fiber    <- Processor.run.fork
      config   <- ZIO.service[AppConfig]
      producer <- InputEventProducer.producer(config.producerSettings, "dormancy_events_v1")
      _        <- producer.publish(UUID.randomUUID().toString, event)
      consumer <- OutputEventConsumer.consumer(
        config.consumerSettings.withClientId(UUID.randomUUID().toString),
        config.processorSettings.outputEvents.schemaRegistrySettings.schemaRegistryUrl,
        config.processorSettings.outputEvents.topics.head,
      )
      queue <- Queue.unbounded[OutputEvent]
      _     <- consumer.stream(r => queue.offer(r.value()).as(())).take(1).runDrain
      list  <- queue.takeAll
      _     <- fiber.interrupt
    } yield assert(list)(hasSize(equalTo(1)))
  }

}

object OutputEventConsumer {

  sealed trait EventConsumer[K, V] {
    def stream(f: ConsumerRecord[K, V] => Task[Unit]): ZStream[Clock & Blocking, Throwable, Unit]
  }

  def consumer(settings: ConsumerSettings, schemaRegistryUrl: String, topic: String): Task[EventConsumer[String, OutputEvent]] = {
    val layer        = ZLayer.fromManaged(Consumer.make(settings))
    val subscription = Subscription.topics(topic)
    val log          = org.log4s.getLogger

    JsonSerialization
      .valueDeserializer[OutputEvent](
        JsonSerializerConfig(schemaRegistryUrl, failInvalidSchema = true, TopicNameStrategy, None),
      )
      .map { serde =>
        new EventConsumer[String, OutputEvent] {
          override def stream(f: ConsumerRecord[String, OutputEvent] => Task[Unit]): ZStream[Clock & Blocking, Throwable, Unit] =
            Consumer
              .subscribeAnd(subscription)
              .plainStream(ZSerde.string, serde)
              .tapError(e => UIO(log.logger.error(e.getMessage, e)))
              .mapM(r => f(r.record).as(r.offset))
              .aggregateAsync(Consumer.offsetBatches)
              .mapM(_.commit)
              .provideSomeLayer(layer)
        }
      }
  }
}

object InputEventProducer {
  sealed trait EventProducer[K, V] {
    def publish(key: K, value: V): RIO[Blocking, RecordMetadata]
  }

  def producer(settings: ProducerSettings, topic: String): RIO[Blocking, EventProducer[String, InputEvent]] = {
    val layer = ZLayer.fromManaged(Producer.make(settings))
    val log   = org.log4s.getLogger

    ZSerde.fromKafkaSerde(InputEvent.kserde, props = Map.empty, isKey = false).map { serde =>
      new EventProducer[String, InputEvent] {
        override def publish(key: String, value: InputEvent): RIO[Blocking, RecordMetadata] = {
          val record = new ProducerRecord(topic, key, value)
          Producer
            .produce(record, ZSerde.string, serde)
            .tapError(e => UIO(log.logger.error(e.getMessage, e)))
            .provideLayer(layer)
        }
      }
    }
  }
}
