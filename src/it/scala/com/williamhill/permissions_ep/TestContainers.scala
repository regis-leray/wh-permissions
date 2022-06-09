package com.williamhill.permissions_ep

import scala.jdk.CollectionConverters.MapHasAsJava
import zio.blocking.{Blocking, effectBlocking}
import zio.{Has, URIO, ZIO}

import com.dimafeng.testcontainers.{KafkaContainer, SchemaRegistryContainer}
import org.testcontainers.containers
import org.testcontainers.containers.Network
object TestContainers {

  def createNetwork: URIO[Blocking, Network] = effectBlocking(Network.newNetwork()).orDie

  def stopNetwork(network: Network): URIO[Blocking, Unit] = effectBlocking(network.close()).orDie

  def startSchemaRegistryContainer: ZIO[Blocking & Has[Network], Nothing, SchemaRegistryContainer] = for {
    network <- ZIO.service[Network]
    container = makeSchemaRegistryContainer(network)
    _ <- effectBlocking(container.start()).orDie
  } yield container

  def stopSchemaRegistryContainer(container: SchemaRegistryContainer): URIO[Blocking, Unit] =
    effectBlocking(container.stop()).orDie

  val kafkaBrokerId         = 1
  private val kafkaHostName = s"kafka$kafkaBrokerId"

  def makeSchemaRegistryContainer(network: Network): SchemaRegistryContainer = SchemaRegistryContainer(network, kafkaHostName)

  // override default dockerImageName, because default behaviour tests for "os.arch" environmental value and in the case of arm64 (M1) architecture, returns another image.
  // But, since M1 users we are using docker server in linux servers, then it should be back the default non-M1 image
  val defaultImage           = "confluentinc/cp-kafka"
  val defaultTag             = "7.0.1"
  val defaultDockerImageName = s"$defaultImage:$defaultTag"

  def startKafkaContainer(container: KafkaContainer): ZIO[Blocking & Has[Network], Nothing, KafkaContainer] = for {
    network <- ZIO.service[Network]
    _       <- ZIO.effect(setupContainer(container, network)).orDie
    _       <- effectBlocking(container.start()).orDie
  } yield container

  def setupContainer(container: KafkaContainer, network: Network): containers.KafkaContainer =
    container.container
      .withNetwork(network)
      .withNetworkAliases(kafkaHostName)
      .withNetworkMode("https")
      .withEnv(
        Map[String, String](
          "KAFKA_BROKER_ID"                 -> kafkaBrokerId.toString,
          "KAFKA_HOST_NAME"                 -> kafkaHostName,
          "KAFKA_AUTO_CREATE_TOPICS_ENABLE" -> "true",
        ).asJava,
      )

  def stopKafkaContainer(container: KafkaContainer): URIO[Blocking, Unit] =
    effectBlocking(container.stop()).orDie

}
