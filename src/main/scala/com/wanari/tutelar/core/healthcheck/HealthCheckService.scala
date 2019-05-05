package com.wanari.tutelar.core.healthcheck

import com.wanari.tutelar.core.healthcheck.HealthCheckService.HealthCheckResult

trait HealthCheckService[F[_]] {
  def getStatus: F[HealthCheckResult]
}

object HealthCheckService {
  import spray.json.DefaultJsonProtocol._
  final case class HealthCheckResult(
      success: Boolean,
      version: String,
      database: Boolean,
      buildAtString: String,
      buildAtMillis: Long,
      commitHash: Option[String]
  )
  implicit val healthCheckResultFormat = jsonFormat6(HealthCheckResult)
}
