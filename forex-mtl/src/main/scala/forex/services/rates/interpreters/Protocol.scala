package forex.services.rates.interpreters

import forex.domain.{Currency, Price, Timestamp}
import forex.http._
import io.circe._
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.deriveConfiguredDecoder

object Protocol {
  implicit val configuration: Configuration = Configuration.default.withSnakeCaseMemberNames

  final case class OneFrameRate(
     from: Currency,
     to: Currency,
     bid: Price,
     ask: Price,
     price: Price,
     timeStamp: Timestamp
  )

  type OneFrameRatesResponse = List[OneFrameRate]

  implicit val oneFrameRateDecoder: Decoder[OneFrameRate] = deriveConfiguredDecoder[OneFrameRate]
}
