/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.selfassessmentassist.config

import com.typesafe.config.Config
import play.api.{ConfigLoader, Configuration}
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.selfassessmentassist.api.models.auth.AuthCredential
import uk.gov.hmrc.selfassessmentassist.utils.Retrying

import java.time.{Duration => JavaDuration}
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.{Duration, FiniteDuration}

trait AppConfig {

  // RDS
  def cipFraudServiceBaseUrl: String
  def cipFraudUsername: String
  def cipFraudToken: String
  def rdsBaseUrlForSubmit: String
  def rdsBaseUrlForAcknowledge: String

  // API Config
  def apiGatewayContext: String
  def confidenceLevelConfig: ConfidenceLevelConfig
  def apiStatus(version: String): String
  def endpointsEnabled(version: String): Boolean
  def featureSwitch: Option[Configuration]

  // SAS
  def rdsSasBaseUrlForAuth: String
  def rdsAuthRequiredForThisEnv: Boolean

  // NRS config items
  def nrsApiKey: String
  def nrsRetries: List[FiniteDuration]
  def appName: String
  def nrsBaseUrl: String
  def nrsFailedBeforeSeconds: Long
  def nrsInProgressRetryAfter: JavaDuration
  def nrsSchedulerInitialDelay: FiniteDuration
  def nrsSchedulerDelay: FiniteDuration
  def rdsAuthCredential: AuthCredential

  // MTD ID lookup
  def mtdIdBaseUrl: String

  // IF
  def ifsBaseUrl: String
  def ifsToken: String
  def ifsEnv: String
  def ifsEnvironmentHeaders: Option[Seq[String]]

  /** Defaults to false
    */
  def endpointAllowsSupportingAgents(endpointName: String): Boolean
}

@Singleton
class AppConfigImpl @Inject() (config: ServicesConfig, val configuration: Configuration) extends AppConfig {

  val appName: String = config.getString("appName")

  // API config items
  def featureSwitch: Option[Configuration]         = configuration.getOptional[Configuration](s"feature-switch")
  val apiGatewayContext: String                    = config.getString("api.gateway.context")
  val confidenceLevelConfig: ConfidenceLevelConfig = configuration.get[ConfidenceLevelConfig](s"api.confidence-level-check")
  def apiStatus(version: String): String           = config.getString(s"api.$version.status")
  def endpointsEnabled(version: String): Boolean   = config.getBoolean(s"feature-switch.version-$version.enabled")

  // NRS config items
  private val nrsConfig                        = configuration.get[Configuration]("microservice.services.non-repudiation")
  val nrsBaseUrl: String                       = config.baseUrl("non-repudiation") + nrsConfig.get[String]("submit-url")
  val nrsApiKey: String                        = nrsConfig.get[String]("x-api-key")
  val nrsFailedBeforeSeconds: Long             = nrsConfig.get[Long]("failed-before-seconds")
  val nrsInProgressRetryAfter: JavaDuration    = nrsConfig.get[JavaDuration]("in-progress-retry-after")
  val nrsSchedulerInitialDelay: FiniteDuration = nrsConfig.get[FiniteDuration]("scheduler.initial-delay")
  val nrsSchedulerDelay: FiniteDuration        = nrsConfig.get[FiniteDuration]("scheduler.delay")

  private val cipConfig                = configuration.get[Configuration]("microservice.services.cip-fraud-service")
  val cipFraudServiceBaseUrl: String   = config.baseUrl("cip-fraud-service") + cipConfig.get[String]("submit-url")
  val cipFraudToken: String            = cipConfig.get[String]("token")
  val cipFraudUsername: String         = cipConfig.get[String]("username")
  private val rdsConfig                = configuration.get[Configuration]("microservice.services.rds")
  val rdsBaseUrlForSubmit: String      = config.baseUrl("rds") + rdsConfig.get[String]("submit-url")
  val rdsBaseUrlForAcknowledge: String = config.baseUrl("rds") + rdsConfig.get[String]("acknowledge-url")

  val rdsSasBaseUrlForAuth: String       = config.baseUrl("rds.sas") + rdsConfig.get[String]("sas.auth-url")
  val rdsAuthRequiredForThisEnv: Boolean = rdsConfig.get[Boolean]("rdsAuthRequiredForThisEnv")

  private val ifsConfig                          = configuration.get[Configuration]("microservice.services.ifs")
  val ifsBaseUrl: String                         = config.baseUrl("ifs") + ifsConfig.get[String]("submit-url")
  val ifsToken: String                           = config.getString("microservice.services.ifs.token")
  val ifsEnv: String                             = config.getString("microservice.services.ifs.env")
  val ifsEnvironmentHeaders: Option[Seq[String]] = configuration.getOptional[Seq[String]]("microservice.services.ifs.environmentHeaders")

  val mtdIdBaseUrl: String = config.baseUrl("mtd-id-lookup")

  def rdsAuthCredential: AuthCredential =
    AuthCredential(
      client_id = rdsConfig.get[String]("sas.clientId"),
      client_secret = rdsConfig.get[String]("sas.clientSecret"),
      grant_type = "client_credentials"
    )

  def nrsRetries: List[FiniteDuration] =
    Retrying.fibonacciDelays(getFiniteDuration(nrsConfig, "initialDelay"), nrsConfig.get[Int]("numberOfRetries"))

  private final def getFiniteDuration(config: Configuration, path: String): FiniteDuration = {
    val string = config.get[String](path)

    Duration.create(string) match {
      case f: FiniteDuration => f
      case _                 => throw new RuntimeException(s"Not a finite duration '$string' for $path")
    }
  }

  def endpointAllowsSupportingAgents(endpointName: String): Boolean =
    supportingAgentEndpoints.getOrElse(endpointName, false)

  private val supportingAgentEndpoints: Map[String, Boolean] =
    configuration
      .getOptional[Map[String, Boolean]]("api.supporting-agent-endpoints")
      .getOrElse(Map.empty)

}

case class ConfidenceLevelConfig(confidenceLevel: ConfidenceLevel, definitionEnabled: Boolean, authValidationEnabled: Boolean)

object ConfidenceLevelConfig {

  implicit val configLoader: ConfigLoader[ConfidenceLevelConfig] = (rootConfig: Config, path: String) => {
    val config = rootConfig.getConfig(path)
    ConfidenceLevelConfig(
      confidenceLevel = ConfidenceLevel.fromInt(config.getInt("confidence-level")).getOrElse(ConfidenceLevel.L200),
      definitionEnabled = config.getBoolean("definition.enabled"),
      authValidationEnabled = config.getBoolean("auth-validation.enabled")
    )
  }

}
