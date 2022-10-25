package forex.http.rates

import forex.domain._
import forex.programs.rates.errors.Error
import forex.programs.rates.errors.Error.RateLookupFailed
import org.http4s.Status.NotFound
import org.http4s.{EntityEncoder, Response}

object Converters {
  import Protocol._

  private[rates] implicit class GetApiResponseOps(val rate: Rate) extends AnyVal {
    def asGetApiResponse: GetApiResponse =
      GetApiResponse(
        from = rate.pair.from,
        to = rate.pair.to,
        price = rate.price,
        timestamp = rate.timestamp
      )
  }

  private [rates] implicit class GetApiErrorResponseOps(val error: Error) extends AnyVal {
    def asErrorResponse[F[_]](implicit encoder: EntityEncoder[F, GetApiErrorResponse]): Response[F] = error match {
      case RateLookupFailed(e) => Response(status = NotFound, body = encoder.toEntity(GetApiErrorResponse(e)).body)
    }
  }

}
