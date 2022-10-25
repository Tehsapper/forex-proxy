package forex.services.rates

import forex.domain.Rate
import errors._
import fs2.Stream

trait Algebra[F[_]] {
  def start(): Stream[F, Unit]
  def get(pair: Rate.Pair): F[Error Either Rate]
}
