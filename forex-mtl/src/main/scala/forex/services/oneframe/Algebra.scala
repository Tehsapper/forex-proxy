package forex.services.oneframe

import forex.domain.Rate
import forex.services.oneframe.errors.Error

trait Algebra[F[_]] {
  def get(pairs: List[Rate.Pair]): F[Error Either List[Rate]]
}
