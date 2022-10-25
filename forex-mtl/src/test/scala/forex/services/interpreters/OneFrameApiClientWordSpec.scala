package forex.services.interpreters

import cats.effect.{IO, Resource}
import cats.implicits.catsSyntaxEitherId
import forex.config.OneFrameApiConfig
import forex.domain.Rate.Pair
import forex.domain.{Currency, Price, Rate, Timestamp}
import forex.services.rates.interpreters.OneFrameApiClient
import forex.services.rates.interpreters.errors.Error.{ApiError, LookupFailure}
import fs2.Stream
import org.http4s.Status.{BadRequest, InternalServerError}
import org.http4s.client.Client
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.{Header, Request, Response}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.{LocalDateTime, OffsetDateTime, ZoneOffset}

class OneFrameApiClientWordSpec extends AnyWordSpec with Matchers {
  "OneFrame API Client" should {
    "properly compose request for currency exchange rate" in {
      val now = Timestamp(OffsetDateTime.of(LocalDateTime.parse("2022-10-21T15:00"), ZoneOffset.ofHours(9)))
      val httpClient = mockHttpClient({ req =>
        req.uri.path should equal ("/rates")
        req.uri.query.toList should contain ("pair" -> Some("USDJPY"))
        req.headers.toList should contain (Header("token", "auth_token"))

        Response[IO](body = Stream.emits("""
          [
            {
              "from":"USD",
              "to":"JPY",
              "bid":0.113,
              "ask":0.861,
              "price":0.15,
              "time_stamp":"2022-10-21T15:00:00+09:00"
            }
          ]
          """.getBytes("UTF-8")))
      }
      )
      val apiClient = new OneFrameApiClient[IO](httpClient, OneFrameApiConfig(uri"http://localhost", "auth_token"))

      val result = apiClient.get(List(Pair(Currency.USD, Currency.JPY))).unsafeRunSync()

      result should equal (List(Rate(Pair(Currency.USD, Currency.JPY), Price(0.15), now)).asRight)
    }

    "emit error in case of non-OK response" in {
      val httpClient = mockHttpClient(_ => Response[IO](status = InternalServerError))
      val apiClient = new OneFrameApiClient[IO](httpClient, OneFrameApiConfig(uri"http://localhost", "auth_token"))

      val result = apiClient.get(List(Pair(Currency.USD, Currency.JPY))).unsafeRunSync()

      result should matchPattern { case Left(LookupFailure(_)) => }
    }

    "emit error in case of reached quota" in {
      val httpClient = mockHttpClient(_ => Response[IO](status = BadRequest, body = Stream.emits("""
          {
            "error": "Quota reached"
          }
          """.getBytes("UTF-8"))))
      val apiClient = new OneFrameApiClient[IO](httpClient, OneFrameApiConfig(uri"http://localhost", "auth_token"))

      val result = apiClient.get(List(Pair(Currency.USD, Currency.JPY))).unsafeRunSync()

      result should matchPattern { case Left(ApiError("Quota reached")) => }
    }
  }

  def mockHttpClient(f: Request[IO] => Response[IO]): Client[IO] = Client.apply[IO] { request =>
    Resource.eval(IO(f(request)))
  }
}
