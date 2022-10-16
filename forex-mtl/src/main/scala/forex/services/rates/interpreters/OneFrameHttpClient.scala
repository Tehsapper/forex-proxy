package forex.services.rates.interpreters

import cats.effect.Sync
import cats.implicits.catsSyntaxEitherId
import cats.syntax.applicativeError._
import cats.syntax.functor._
import forex.config.OneFrameApiConfig
import forex.domain.Rate
import forex.domain.Rate.Pair
import forex.http._
import forex.services.rates.errors.Error.OneFrameLookupFailed
import forex.services.rates.interpreters.Protocol.{rateDecoder, Rate => OneFrameRate}
import forex.services.rates.{Algebra, errors}
import org.http4s.QueryParamEncoder._
import org.http4s._
import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.client._

class OneFrameHttpClient[F[_]: Sync](
  httpClient: Client[F], config: OneFrameApiConfig
) extends Algebra[F] {

  override def get(pair: Rate.Pair): F[errors.Error Either Rate] = {
    httpClient.expect[List[OneFrameRate]](makeRequest(pair))
      .map(rates => {
        val rate = rates.head
        Rate(Pair(rate.from, rate.to), rate.price, rate.timeStamp)
      })
      .map(_.asRight[errors.Error])
      .handleError(e => OneFrameLookupFailed(e.toString).asLeft)
  }

  private def makeRequest(pair: Rate.Pair): Request[F] =
    Request(uri = makeRequestUrl(pair), headers = Headers.of(Header("token", config.token)))

  private def makeRequestUrl(pair: Rate.Pair): Uri =
    Uri.fromString(config.baseUrl)
      .map(_.withQueryParam("pair", s"${pair.from}${pair.to}"))
      .getOrElse(throw new IllegalStateException("Invalid base URL in configuration"))
}
