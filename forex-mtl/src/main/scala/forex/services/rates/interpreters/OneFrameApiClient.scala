package forex.services.rates.interpreters

import cats.effect.Sync
import cats.implicits._
import forex.config.OneFrameApiConfig
import forex.domain.Rate
import forex.domain.Rate.Pair
import forex.http._
import forex.services.rates.errors.Error
import forex.services.rates.errors.Error.LookupFailure
import forex.services.rates.interpreters.Protocol.{OneFrameRatesResponse, oneFrameRateDecoder}
import org.http4s.QueryParamEncoder._
import org.http4s._
import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.client._

class OneFrameApiClient[F[_]: Sync](
  httpClient: Client[F], config: OneFrameApiConfig
) {
  private val ratesUrl = config.baseUrl.addPath("/rates")

  def get(pairs: List[Rate.Pair]): F[Error Either List[Rate]] = {
    httpClient.expect[OneFrameRatesResponse](makeRequest(pairs))
      .map(_.map { rate =>
        Rate(Pair(rate.from, rate.to), rate.price, rate.timeStamp)
      }.asRight[Error])
      .handleError(e => LookupFailure(e).asLeft)
  }

  private def makeRequest(pairs: List[Rate.Pair]): Request[F] =
    Request(uri = makeRequestUrl(pairs), headers = Headers.of(Header("token", config.token)))

  private def makeRequestUrl(pairs: List[Rate.Pair]): Uri =
    ratesUrl.withQueryParam("pair", pairs.map(p => s"${p.from}${p.to}"))
}
