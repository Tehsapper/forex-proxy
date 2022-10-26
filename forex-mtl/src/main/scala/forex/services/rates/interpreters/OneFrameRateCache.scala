package forex.services.rates.interpreters

import cats.effect._
import cats.implicits._
import forex.config.OneFrameRateCacheConfig
import forex.domain.Rate.Pair
import forex.domain.{Currency, Rate}
import forex.services.OneFrameService
import forex.services.rates.Algebra
import forex.services.rates.errors.Error.NoRateFound
import forex.services.rates.errors._
import fs2.Stream
import org.slf4j.LoggerFactory
import scalacache.{Cache, Mode}

import scala.concurrent.duration.{DurationLong, NANOSECONDS}

class OneFrameRateCache[F[_]: Timer: Concurrent](
  oneFrameService: OneFrameService[F],
  underlyingCache: Cache[(Long, Rate)],
  config: OneFrameRateCacheConfig
) extends Algebra[F] {
  implicit val cache: Cache[(Long, Rate)] = underlyingCache
  implicit val mode: Mode[F] = scalacache.CatsEffect.modes.async

  private val logger = LoggerFactory.getLogger(this.getClass)

  private val allCurrencyCombinations = Currency.all.combinations(2)
    .flatMap {
      case Seq(a, b) => List(Pair(a, b), Pair(b, a))
      case _ => throw new IllegalStateException("at least one currency pair combination should exist")
    }.toList

  override def start(): Stream[F, Unit] = {
    Stream
      .eval(refresh())
      .concurrently(Stream.awakeEvery[F](config.refreshPeriod).evalMap(_ => refresh()))
      .handleErrorWith { e =>
        logger.error("failed to update cache", e)
        // TODO: more sophisticated retry policy (e.g. jitter + exponential backoff)
        start().delayBy(config.retryPeriod)
      }
  }

  override def get(pair: Pair): F[Error Either Rate] = {
    // Unfortunately, TTL setting is broken in scalacache 0.28 (see https://github.com/cb372/scalacache/issues/522)
    // It is fixed in *pre-release* 1.0.0-M5 version, but using it would require a massive dependencies update.
    for {
      now <- monotonicTime
      cached <- scalacache.get(pair)
    } yield cached match {
      case None => NoRateFound(pair).asLeft[Rate]
      case Some((timestamp, _)) if (now - timestamp).nanos > config.ttl => NoRateFound(pair).asLeft[Rate]
      case Some((_, rate)) => rate.asRight[Error]
    }
  }

  private def refresh(): F[Unit] = {
    for {
      now <- monotonicTime
      result <- oneFrameService.get(allCurrencyCombinations)
      rates <- Sync[F].fromEither(result)
      _ <- rates.traverse(rate => scalacache.put(rate.pair)(now -> rate))
      _ <- Sync[F].delay(logger.info("Updated cache"))
    } yield ()
  }

  private def monotonicTime(implicit timer: Timer[F]) = timer.clock.monotonic(NANOSECONDS)
}
