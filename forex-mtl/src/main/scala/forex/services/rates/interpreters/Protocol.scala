package forex.services.rates.interpreters

import forex.domain.{Currency, Price, Timestamp}
import forex.http._
import io.circe._
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.deriveConfiguredDecoder

object Protocol {
  implicit val configuration: Configuration = Configuration.default.withSnakeCaseMemberNames

  final case class Rate(
     from: Currency,
     to: Currency,
     bid: Price,
     ask: Price,
     price: Price,
     timeStamp: Timestamp
  )

  type RatesResponse = List[Rate]

  implicit val rateDecoder: Decoder[Rate] = deriveConfiguredDecoder[Rate]
}
