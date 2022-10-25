package forex.programs.rates

import forex.services.rates.errors.{ Error => RatesServiceError }

object errors {

  sealed trait Error extends Exception
  object Error {
    final case class RateLookupFailed(msg: String) extends Error
  }

  def toProgramError(error: RatesServiceError): Error = error match {
    case RatesServiceError.LookupFailure(e) => Error.RateLookupFailed(e.getMessage)
    case RatesServiceError.NoRateFound(_) => Error.RateLookupFailed("no exchange rate")
  }

}
