package com.virtuslab

import cats.effect.std.Dispatcher
import cats.effect.{ExitCode, IO, IOApp}
import sttp.tapir.server.netty.cats.{NettyCatsServer, NettyCatsServerOptions}

import java.net.InetSocketAddress
import scala.io.StdIn

object Main extends IOApp:
  val port = sys.env.get("http.port").map(_.toInt).getOrElse(8080)

  override def run(args: List[String]): IO[ExitCode] =
    Dispatcher[IO]
      .map(d => {
        NettyCatsServer.apply[IO, InetSocketAddress]({
          NettyCatsServerOptions
            .customiseInterceptors(d)
            .metricsInterceptor(Endpoints.prometheusMetrics.metricsInterceptor())
            .options
        })
      })
      .use { server =>
        for
          bind <- server
            .port(port)
            .host("localhost")
            .addEndpoints(Endpoints.all)
            .start()
          _ <- IO.blocking {
            println(s"Go to http://localhost:${bind.port}/docs to open SwaggerUI. Press ENTER key to exit.")
            StdIn.readLine()
          }
          _ <- bind.stop()
        yield bind
      }
      .as(ExitCode.Success)
