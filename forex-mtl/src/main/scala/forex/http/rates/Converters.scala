package forex.http.rates

import cats.data.NonEmptyList
import cats.effect.Sync
import cats.implicits.catsSyntaxApplicativeId
import forex.domain._
import forex.programs.rates.errors.Error
import forex.programs.rates.errors.Error.RateLookupFailed
import org.http4s.Status.{BadRequest, NotFound}
import org.http4s.{EntityEncoder, ParseFailure, Response, Status}

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

  private[rates] implicit class GetApiErrorResponseOps(val error: Error) extends AnyVal {
    def asErrorResponse[F[_]: Sync]: F[Response[F]] = error match {
      case RateLookupFailed(msg) => toErrorResponse(NotFound, GetApiErrorResponse(msg))
    }
  }

  private[rates] def toErrorResponse[F[_]: Sync](name: String, pfs: NonEmptyList[ParseFailure]): F[Response[F]] =
    toErrorResponse(BadRequest, GetApiErrorResponse(s"$name - ${pfs.head.sanitized}"))

  private def toErrorResponse[F[_]: Sync](status: Status, error: GetApiErrorResponse)
                                         (implicit encoder: EntityEncoder[F, GetApiErrorResponse]): F[Response[F]] =
    Response(status, body = encoder.toEntity(error).body).pure[F]

}
