package forex.services.rates

import cats.effect.Sync
import forex.config.OneFrameApiConfig
import interpreters._
import org.http4s.client.Client

object Interpreters {
  def live[F[_]: Sync](config: OneFrameApiConfig, httpClient: Client[F]): Algebra[F] =
    new OneFrameHttpClient[F](httpClient, config)
}
