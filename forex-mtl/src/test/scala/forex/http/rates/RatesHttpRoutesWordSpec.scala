package forex.http.rates

import cats.effect.{IO, Sync}
import cats.implicits._
import forex.domain.Rate.Pair
import forex.domain.{Currency, Price, Rate, Timestamp}
import forex.programs.RatesProgram
import forex.programs.rates.Protocol.GetRatesRequest
import forex.programs.rates.errors.Error
import forex.programs.rates.errors.Error.RateLookupFailed
import io.circe._
import io.circe.parser._
import org.http4s.Status.{NotFound, Ok}
import org.http4s.circe._
import org.http4s.implicits._
import org.http4s.{Method, Query, Request, Uri}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.{LocalDateTime, OffsetDateTime, ZoneOffset}

class RatesHttpRoutesWordSpec extends AnyWordSpec with Matchers {
  "Rates Http Routes" should {
    "return exchange rate if it exists for provided currency pair" in {
      val now = Timestamp(OffsetDateTime.of(LocalDateTime.parse("2022-10-21T15:00"), ZoneOffset.ofHours(9)))
      val existingRate = Rate(Pair(Currency.USD, Currency.JPY), Price(150.5), now)
      val program = createDummyProgram(existingRate.asRight[Error])
      val routes = createRoutes(program)

      val result = routes
        .run(makeGetRatesRequest(existingRate.pair))
        .unsafeRunSync()

      result.status should equal (Ok)
      result.as[Json].unsafeRunSync() should equal (json("""
        {
          "from": "USD",
          "to": "JPY",
          "price": 150.5,
          "timestamp": "2022-10-21T15:00:00+09:00"
        }
      """))
    }

    "return error if no rate for provided currency pair" in {
      val program = createDummyProgram(RateLookupFailed("no exchange rate").asLeft[Rate])
      val routes = createRoutes(program)

      val result = routes
        .run(makeGetRatesRequest(Pair(Currency.USD, Currency.JPY)))
        .unsafeRunSync()

      result.status should equal (NotFound)
      result.as[Json].unsafeRunSync() should equal (json("""
        {
          "error": "no exchange rate"
        }
      """))
    }

    "return error if unsupported currency provided" in {
      val program = createDummyProgram(RateLookupFailed("").asLeft[Rate])
      val routes = createRoutes(program)

      val result = routes
        .run(makeGetRatesRequest("XYZ", "YYY"))
        .unsafeRunSync()

      result.status should equal (NotFound)
      result.as[Json].unsafeRunSync() should equal (json("""
        {
          "error": "no exchange rate"
        }
      """))
    }
  }

  private def json(raw: String): Json =
    parse(raw).getOrElse(Json.Null)

  private def makeGetRatesRequest(pair: Pair): Request[IO] =
    makeGetRatesRequest(pair.from.show, pair.to.show)

  private def makeGetRatesRequest(from: String, to: String): Request[IO] =
    Request[IO](Method.GET, Uri(path = "rates", query = Query.fromPairs(
      "from" -> from,
      "to" -> to)))

  private def createDummyProgram(rateResult: Either[Error, Rate]): RatesProgram[IO] =
    (_: GetRatesRequest) => rateResult.pure[IO]

  private def createRoutes[F[_]: Sync](program: RatesProgram[F]) =
    new RatesHttpRoutes(program).routes.orNotFound

}
