package com.williamhill.permission

import com.williamhill.permission.application.Env
import com.williamhill.permission.config.AppConfig
import com.williamhill.permission.endpoints.Effect
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.{Status, Uri}
import zio.blocking.Blocking
import zio.clock.Clock
import zio.interop.catz.*
import zio.magic.*
import zio.random.Random
import zio.test.*
import zio.test.Assertion.*
import zio.{Has, ZIO, ZLayer}

object PermissionEpSpec extends DefaultRunnableSpec {

  private val testConfig: ZLayer[Blocking & Random, Throwable, Has[AppConfig]] = ZIO
    .service[Random.Service]
    .flatMap(_.nextLongBetween(1024, 65535))
    .toLayer
    .flatMap { port =>
      AppConfig.live.update[AppConfig] { config =>
        val hc = config.healthcheck
        config.copy(healthcheck = hc.copy(port = port.get.toInt))
      }
    }

  override def spec: ZSpec[Environment, Failure] =
    suite("Permission app")(
      testM("Starts and answers health check") {
        assertM(
          BlazeClientBuilder[Effect].resource.toManagedZIO.zipPar(PermissionEp.server <*> PermissionEp.ep.fork).use {
            case (client, (srv, ep)) =>
              val port = srv.address.getPort
              val uri  = Uri.unsafeFromString(s"http://localhost:$port") / "permissions-ep" / "meta" / "health"
              client.statusFromUri(uri) <* ep.interrupt
          },
        )(equalTo(Status.Ok))
          .inject(
            Blocking.live,
            zio.console.Console.live,
            Random.live,
            Clock.live,
            Env.core,
            testConfig,
          )
      }
        .mapError(TestFailure.fail),
    )
}
