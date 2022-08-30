package com.williamhill.permission

import com.williamhill.permission.kafka.events.generic.InputEvent
import com.williamhill.permission.kafka.events.generic.OutputEvent.OutputEvent
import io.circe.Decoder
import io.circe.parser.parse
import zio.ZIO
import zio.blocking.Blocking
import zio.nio.file.{Files, Path}
import zio.stream.{ZSink, ZStream}
import zio.test.*
import zio.test.Assertion.*
import zio.test.environment.TestEnvironment
import java.io.IOException

object EventRuleProcessorSpec extends DefaultRunnableSpec {
  private val baseFolder = Path(getClass.getClassLoader.getResource("functional-tests/").toURI)

  private def readFiles[A: Decoder](p: Path): ZStream[Blocking, Exception, (A, Path)] =
    ZStream
      .fromEffect(Files.list(p).runCollect.map(c => c.sortBy(_.filename.toString())))
      .flatMap(ZStream.fromChunk(_))
      .mapM(readFile[A])

  private def readFile[A: Decoder](p: Path): ZIO[Blocking, Exception, (A, Path)] =
    Files
      .readAllLines(p)
      .map(_.mkString)
      .flatMap(s =>
        ZIO
          .fromEither(parse(s).flatMap(_.as[A]))
          .mapError(err => new IOException(s"Cannot parse file ${p.toFile.getAbsolutePath} due to ${err.getMessage}")),
      )
      .map(_.->(p))

  private val source = readFiles[InputEvent](baseFolder / "in")
    .zip(readFiles[OutputEvent](baseFolder / "out"))
    .run(ZSink.collectAll)

  val assertFails: String => TestResult = label => assert(false)(nothing ?? label)

  override def spec: ZSpec[TestEnvironment, Any] = suiteM("EventRuleProcessorSpec")(specs)

  private val processor = EventRuleProcessor()

  private val specs = source
    .map(_.map { case ((input, inputFile), (expected, _)) =>
      testM(s"Evaluate all permission rules for ${inputFile.filename} event json file") {
        for {
          outputs <- processor.handle(input)
        } yield {
          val maybeEvent = outputs.headOption.map(_._2)

          (outputs.map(_._1) match {
            case _ :: Nil => assertCompletes
            case Nil =>
              assertFails(s"Error in Permission Rules ! No rules match for json in/${inputFile.filename}. Please verify your Rule file")
            case others =>
              assertFails(
                s"Error in Permission Rules ! Duplicate rules found ${others.map(_.name).mkString("[", ",", "]")} for json file in/${inputFile.filename}",
              )
          }) && assert(maybeEvent.map(_.header))(isSome(equalTo(expected.header)))
          && assert (maybeEvent.map(_.body.newValues.id))(isSome(equalTo(expected.body.newValues.id)))
          && assert (maybeEvent.map(_.body.newValues.universe))(isSome(equalTo(expected.body.newValues.universe)))
          && assert (maybeEvent.map(_.body.`type`))(isSome(equalTo(expected.body.`type`)))
          && assert (maybeEvent
            .map(_.body.newValues.data.permissionDenials))(isSome(hasSameElementsDistinct(expected.body.newValues.data.permissionDenials)))
          && assert (maybeEvent.map(_.body.newValues.data.actions.map(a => a.name -> a.`type`)))(
            isSome(hasSameElementsDistinct(expected.body.newValues.data.actions.map(a => a.name -> a.`type`))),
          )
          && assert (maybeEvent.map(_.body.newValues.data.actions.flatMap(_.relatesToPermissions)))(
            isSome(hasSameElementsDistinct(expected.body.newValues.data.actions.flatMap(_.relatesToPermissions))),
          )
        }
      }
    })
    .mapError(TestFailure.fail)
}
