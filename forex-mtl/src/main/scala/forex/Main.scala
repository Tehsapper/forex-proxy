package forex

import cats.effect._
import forex.config._
import fs2.Stream
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.client.middleware.{Retry, RetryPolicy}
import org.http4s.server.blaze.BlazeServerBuilder

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    new Application[IO].stream(executionContext).compile.fold(ExitCode.Success)((_, code) => code)

}

class Application[F[_]: ConcurrentEffect: Timer] {

  def stream(ec: ExecutionContext): Stream[F, ExitCode] =
    for {
      config <- Config.stream("app")
      httpClient <- BlazeClientBuilder[F](ec).stream.map(client =>
        Retry[F](RetryPolicy[F](RetryPolicy.exponentialBackoff(maxRetry = 3, maxWait = 5.seconds)))(client))
      module = new Module[F](config, httpClient)
      _ <- module.start
      statusCode <- BlazeServerBuilder[F](ec)
            .bindHttp(config.http.port, config.http.host)
            .withHttpApp(module.httpApp)
            .serve
    } yield statusCode

}
