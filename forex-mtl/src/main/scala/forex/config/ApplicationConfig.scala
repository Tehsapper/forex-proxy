package forex.config

import org.http4s.Uri

import scala.concurrent.duration.FiniteDuration

case class ApplicationConfig(
    http: HttpConfig,
    oneFrame: OneFrameConfig
)

case class HttpConfig(
    host: String,
    port: Int,
    timeout: FiniteDuration
)

case class OneFrameConfig(
    api: OneFrameApiConfig,
    cache: OneFrameRateCacheConfig
)

case class OneFrameApiConfig(
    baseUrl: Uri,
    token: String
)

case class OneFrameRateCacheConfig(
    refreshPeriod: FiniteDuration
)
