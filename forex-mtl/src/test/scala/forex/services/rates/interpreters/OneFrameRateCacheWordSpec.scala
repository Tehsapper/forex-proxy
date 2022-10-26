package forex.services.rates.interpreters

import cats.effect.laws.util.TestContext
import cats.effect.{ContextShift, IO, Timer}
import cats.implicits._
import forex.config.OneFrameRateCacheConfig
import forex.domain.Rate.Pair
import forex.domain.{Currency, Price, Rate, Timestamp}
import forex.services.OneFrameService
import forex.services.oneframe.errors.Error.ApiError
import forex.services.oneframe.errors.{Error => OneFrameError}
import forex.services.rates.errors.Error
import forex.services.rates.errors.Error.NoRateFound
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import scalacache.Mode
import scalacache.caffeine.CaffeineCache

import scala.concurrent.duration.{DurationInt, FiniteDuration, NANOSECONDS}

class OneFrameRateCacheWordSpec extends AnyWordSpec with Matchers {
  implicit lazy val ec: TestContext = TestContext.apply()
  implicit lazy val cs: ContextShift[IO] = IO.contextShift(ec)
  implicit lazy val ioTimer: Timer[IO] = ec.timer[IO]
  implicit val mode: Mode[IO] = scalacache.CatsEffect.modes.async

  "OneFrame Rate Cache" should {
    "return nothing on cache miss" in {
      val underlyingCache = CaffeineCache[(Long, Rate)]

      val oneFrameRateCache = new OneFrameRateCache[IO](
        mockOneFrameInterpreter(ApiError("Quota exceeded").asLeft[List[Rate]]),
        underlyingCache,
        cacheConfigWithTtlOf(5.minutes)
      )

      val result = oneFrameRateCache.get(Pair(Currency.JPY, Currency.USD)).unsafeRunSync()

      result should matchPattern { case Left(NoRateFound(_)) => }
    }

    "return nothing if cached rate expired" in {
      val jpyUsd = Pair(Currency.JPY, Currency.USD)
      val underlyingCache = CaffeineCache[(Long, Rate)]
      underlyingCache.put(jpyUsd)(monotonicTime -> Rate(jpyUsd, Price(150.0), Timestamp.now)).unsafeRunSync()

      val oneFrameRateCache = new OneFrameRateCache[IO](
        mockOneFrameInterpreter(ApiError("Quota exceeded").asLeft[List[Rate]]),
        underlyingCache,
        cacheConfigWithTtlOf(5.minutes)
      )

      ec.tick(10.minutes)

      val result = oneFrameRateCache.get(jpyUsd).unsafeRunSync()

      result should matchPattern { case Left(NoRateFound(_)) => }
    }

    "return rate if cached and not expired" in {
      val jpyUsd = Pair(Currency.JPY, Currency.USD)
      val rate = Rate(jpyUsd, Price(150.0), Timestamp.now)

      val underlyingCache = CaffeineCache[(Long, Rate)]
      underlyingCache.put(jpyUsd)(monotonicTime -> rate).unsafeRunSync()

      val oneFrameRateCache = new OneFrameRateCache[IO](
        mockOneFrameInterpreter(ApiError("Quota exceeded").asLeft[List[Rate]]),
        underlyingCache,
        cacheConfigWithTtlOf(5.minutes)
      )

      ec.tick(2.minutes)

      val result = oneFrameRateCache.get(jpyUsd).unsafeRunSync()

      result should equal (rate.asRight[Error])
    }
  }

  "OneFrame Cache Update" should {
    "attempt to populate cache on startup" in {
      val jpyUsd = Pair(Currency.JPY, Currency.USD)
      val rate = Rate(jpyUsd, Price(150.0), Timestamp.now)

      val underlyingCache = CaffeineCache[(Long, Rate)]
      val mockOneFrameService = new CountingMockOneFrameService(_ => List(rate).asRight[OneFrameError])
      val oneFrameRateCache = new OneFrameRateCache[IO](mockOneFrameService, underlyingCache, cacheConfig)

      oneFrameRateCache.start().compile.drain.unsafeToFuture()
      ec.tick()

      mockOneFrameService.calls should equal (1)
      underlyingCache.get(jpyUsd).unsafeRunSync() should matchPattern {
        case Some((_, cachedRate)) if cachedRate == rate =>
      }
    }
  }

  private def mockOneFrameInterpreter(response: Either[OneFrameError, List[Rate]]): OneFrameService[IO] =
    (_: List[Rate.Pair]) => IO.pure(response)

  private class CountingMockOneFrameService(f: List[Rate.Pair] => Either[OneFrameError, List[Rate]]) extends OneFrameService[IO] {
    private var counter = 0

    override def get(pairs: List[Pair]): IO[Either[OneFrameError, List[Rate]]] = {
      counter += 1
      IO.pure(f(pairs))
    }

    def calls: Int = counter
  }

  private def monotonicTime = ioTimer.clock.monotonic(NANOSECONDS).unsafeRunSync()

  private def cacheConfig = OneFrameRateCacheConfig(5.minutes, 5.seconds, 5.minutes)
  private def cacheConfigWithTtlOf(ttl: FiniteDuration) = OneFrameRateCacheConfig(5.minutes, 5.seconds, ttl)
}
