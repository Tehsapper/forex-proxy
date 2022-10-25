package forex.http.rates

import cats.implicits.catsSyntaxEitherId
import forex.domain.Currency
import org.http4s.{ParseFailure, QueryParamDecoder}
import org.http4s.dsl.impl._

object QueryParams {

  private[http] implicit val currencyQueryParam: QueryParamDecoder[Currency] =
    QueryParamDecoder[String].emap(raw => Currency.fromString(raw) match {
      case Some(currency) => currency.asRight[ParseFailure]
      case None => ParseFailure("unsupported currency", s"failed to parse $raw as currency").asLeft[Currency]
    })

  object FromQueryParam extends ValidatingQueryParamDecoderMatcher[Currency]("from")
  object ToQueryParam extends ValidatingQueryParamDecoderMatcher[Currency]("to")

}
