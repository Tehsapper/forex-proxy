package forex.services.rates

import cats.effect.{Concurrent, Timer}
import forex.config.OneFrameRateCacheConfig
import forex.domain.Rate
import forex.services.OneFrameService
import forex.services.rates.interpreters.OneFrameRateCache
import scalacache.caffeine.CaffeineCache

object Interpreters {
  def live[F[_]: Timer: Concurrent](oneFrameService: OneFrameService[F], config: OneFrameRateCacheConfig): Algebra[F] =
    new OneFrameRateCache[F](oneFrameService, CaffeineCache[(Long, Rate)], config)
}
