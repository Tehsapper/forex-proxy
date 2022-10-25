package forex.config

import cats.effect.Sync
import fs2.Stream
import org.http4s.Uri
import pureconfig.error.{CannotConvert, ConfigReaderFailures}
import pureconfig.{ConfigReader, ConfigSource}
import pureconfig.generic.auto._

object Config {

  implicit val uriReader: ConfigReader[Uri] = ConfigReader.fromCursor[Uri] { cur =>
    cur.asString.flatMap { raw =>
      Uri.fromString(raw).left.map(failure =>
        ConfigReaderFailures(cur.failureFor(CannotConvert(raw, "Uri", failure.details))))}
  }

  /**
   * @param path the property path inside the default configuration
   */
  def stream[F[_]: Sync](path: String): Stream[F, ApplicationConfig] = {
    Stream.eval(Sync[F].delay(
      ConfigSource.default.at(path).loadOrThrow[ApplicationConfig]))
  }

}
