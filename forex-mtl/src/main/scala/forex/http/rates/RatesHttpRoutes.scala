package forex.http
package rates

import cats.effect.Sync
import cats.implicits._
import forex.programs.RatesProgram
import forex.programs.rates.{Protocol => RatesProgramProtocol}
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router

class RatesHttpRoutes[F[_]: Sync](rates: RatesProgram[F]) extends Http4sDsl[F] {

  import Converters._
  import Protocol._
  import QueryParams._

  private[http] val prefixPath = "/rates"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root :? FromQueryParam(fromResult) +& ToQueryParam(toResult) =>
      fromResult.fold(pfs => toErrorResponse("from", pfs), from =>
        toResult.fold(pfs => toErrorResponse("to", pfs), to =>
          rates.get(RatesProgramProtocol.GetRatesRequest(from, to)).flatMap {
            case Left(error) => error.asErrorResponse
            case Right(rate) => Ok(rate.asGetApiResponse)
          }
        )
      )
  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )

}
