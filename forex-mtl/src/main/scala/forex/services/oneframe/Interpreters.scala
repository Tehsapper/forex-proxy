package forex.services.oneframe

import cats.effect.Sync
import forex.config.OneFrameApiConfig
import forex.services.oneframe.interpreters.OneFrameApiClient
import org.http4s.client.Client

object Interpreters {
  def live[F[_]: Sync](httpClient: Client[F], config: OneFrameApiConfig): Algebra[F] =
    new OneFrameApiClient[F](httpClient, config)
}
