package forex.services.rates.interpreters

import cats.syntax.functor._
import forex.domain.{Currency, Price, Timestamp}
import forex.http._
import io.circe._
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveUnwrappedDecoder}

object Protocol {
  implicit val configuration: Configuration = Configuration.default.withSnakeCaseMemberNames

  sealed trait OneFrameResponse

  final case class OneFrameRate(
     from: Currency,
     to: Currency,
     bid: Price,
     ask: Price,
     price: Price,
     timeStamp: Timestamp
  )

  final case class OneFrameRates(rates: List[OneFrameRate]) extends OneFrameResponse

  final case class OneFrameError(
    error: String
  ) extends OneFrameResponse

  implicit val oneFrameRateDecoder: Decoder[OneFrameRate] = deriveConfiguredDecoder[OneFrameRate]
  implicit val oneFrameRatesDecoder: Decoder[OneFrameRates] = deriveUnwrappedDecoder[OneFrameRates]
  implicit val oneFrameErrorDecoder: Decoder[OneFrameError] = deriveConfiguredDecoder[OneFrameError]

  implicit val oneFrameResponseDecoder: Decoder[OneFrameResponse] =
    List[Decoder[OneFrameResponse]](
      Decoder[OneFrameRates].widen,
      Decoder[OneFrameError].widen
    ).reduceLeft(_ or _)
}
