package forex.services.oneframe

object errors {

  sealed trait Error extends Exception
  object Error {
    final case class LookupFailure(e: Throwable) extends Error {
      override def getMessage: String = e.getMessage
    }

    // might be worthwhile to also make this a sealed trait, but OneFrame API spec is unknown.
    final case class ApiError(error: String) extends Error {
      override def getMessage: String = error
    }
  }

}
