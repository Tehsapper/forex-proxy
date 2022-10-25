package forex.services.rates

import forex.domain.Rate.Pair

object errors {

  sealed trait Error extends Exception
  object Error {
    final case class LookupFailure(e: Throwable) extends Error {
      override def getMessage: String = e.getMessage
    }
    final case class NoRateFound(pair: Pair) extends Error
  }

}
