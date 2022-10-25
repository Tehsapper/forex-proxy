package forex.services.rates

import cats.effect.{Concurrent, Timer}
import forex.config.OneFrameRateCacheConfig
import forex.services.rates.cache.OneFrameRateCache
import forex.services.rates.interpreters._

object Interpreters {
  def live[F[_]: Timer: Concurrent](
    oneFrameApiClient: OneFrameApiClient[F],
    oneFrameRateCacheConfig: OneFrameRateCacheConfig
  ): Algebra[F] =
    new OneFrameRateCache[F](oneFrameApiClient, oneFrameRateCacheConfig)
}
