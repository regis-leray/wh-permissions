package com.williamhill.permission

import java.io.File

import scala.io.Source
import scala.util.Using

import com.typesafe.scalalogging.LazyLogging
import com.williamhill.permission.application.config.{MappingsConfig, RulesConfig}
import com.williamhill.permission.kafka.events.generic.InputEvent
import com.williamhill.platform.event.permission.Event as OutputEvent
import io.circe.Decoder
import io.circe.parser.parse
import zio.blocking.Blocking
import zio.clock.Clock
import zio.test.Assertion.*
import zio.test.environment.TestEnvironment
import zio.test.{DefaultRunnableSpec, ZSpec, assert}
import zio.{Has, URLayer, ZIO, ZLayer}

object EventProcessorSpec extends DefaultRunnableSpec with LazyLogging {

  private val configsLayer: URLayer[Blocking, Has[MappingsConfig] & Has[RulesConfig]] =
    MappingsConfig.layer ++ RulesConfig.layer

  private val componentsLayer: URLayer[Has[MappingsConfig] & Has[RulesConfig] & Clock, Has[FacetContextParser] & Has[PermissionLogic]] =
    FacetContextParser.layer ++ PermissionLogic.layer

  private val eventProcessorLayer: URLayer[Blocking & Clock, Has[EventProcessor]] =
    (configsLayer ++ ZLayer.identity[Clock]) >+>
      componentsLayer >>>
      EventProcessor.layer

  private val baseFolder: File =
    new File(getClass.getClassLoader.getResource("functional-tests").getFile).ensuring(_.exists())

  private def listFiles(file: File): List[File] =
    List.from(file.ensuring(_.isDirectory).listFiles())

  private def testFiles: Map[String, List[(File, File)]] = listFiles(baseFolder)
    .map(f => f.getName -> listFiles(f))
    .flatMap { case (eventName, files) =>
      def allFiles(name: String): Option[List[File]] =
        files
          .find(_.getName == name)
          .map(listFiles)
          .map(_.filter(_.isFile))

      for {
        inputFiles  <- allFiles("in")
        outputFiles <- allFiles("out")

        inputFileNames  = inputFiles.map(_.getName)
        outputFileNames = outputFiles.map(_.getName)

        _ = inputFileNames
          .diff(outputFileNames)
          .foreach(fileName => logger.warn(s"Expected outcome missing for $eventName/$fileName: skipped"))
        _ = outputFileNames
          .diff(inputFileNames)
          .foreach(fileName => logger.warn(s"Input file missing for $eventName/$fileName: skipped"))

        inAndOut = inputFiles.flatMap(inputFile => outputFiles.find(_.getName == inputFile.getName).map(inputFile -> _))
      } yield eventName -> inAndOut
    }
    .toMap

  private def fetchJson[T: Decoder](file: File): Either[String, T] =
    for {
      content <- Using(Source.fromFile(file))(_.getLines().mkString).toEither.left
        .map(ex => s"Can't read ${file.getPath}: ${ex.getMessage}")
      json    <- parse(content).left.map(ex => s"${file.getName} doesn't contain valid JSON: ${ex.message}")
      decoded <- json.as[T].left.map(ex => s"Cannot parse content for ${file.getPath}: ${ex.history}")
    } yield decoded

  private val tests = testFiles.flatMap { case (topicName, ios) =>
    ios.map { case (inputFile, outputFile) =>
      val scenarioName = inputFile.getName.takeWhile(_ != '.')

      val spec: ZSpec[Has[EventProcessor], String] =
        testM(s"$topicName/$scenarioName") {
          for {
            inputEvent  <- ZIO.fromEither(fetchJson[InputEvent](inputFile))
            outputEvent <- ZIO.fromEither(fetchJson[OutputEvent](outputFile))
            processed   <- EventProcessor.handleInput(topicName, inputEvent).mapError(_.message).either
            sameHeader = assert(processed.map(_.header))(isRight(equalTo(outputEvent.header)))
            sameBody   = assert(processed.map(_.body))(isRight(equalTo(outputEvent.body)))
          } yield sameHeader && sameBody
        }

      spec.provideSomeLayerShared(eventProcessorLayer)
    }
  }.toList

  override val spec: ZSpec[TestEnvironment, Any] =
    suite("EventProcessor")(tests*)

}
