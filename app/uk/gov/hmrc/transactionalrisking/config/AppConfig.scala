/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.transactionalrisking.config

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.transactionalrisking.utils.Retrying

import scala.concurrent.duration.{Duration, FiniteDuration}

@Singleton
class AppConfig @Inject()(config: ServicesConfig,configuration: Configuration) {

  val appName: String = config.getString("appName")

  // NRS config items
  val nrsApiKey: String = config.getString("access-keys.xApiKey")
  private val nrsConfig = configuration.get[Configuration]("microservice.services.non-repudiation")
  val nrsBaseUrl: String = config.baseUrl("non-repudiation")+nrsConfig.get[String]("submit-url")

  private val rdsConfig = configuration.get[Configuration]("microservice.services.rds")
  val rdsBaseUrlForSubmit:String = config.baseUrl("rds")+rdsConfig.get[String]("submit-url")
  val rdsBaseUrlForAcknowledge:String = config.baseUrl("rds")+rdsConfig.get[String]("acknowledge-url")

  private val cipConfig = configuration.get[Configuration]("microservice.services.cip-fraud-service")
  val cipFraudServiceBaseUrl:String = config.baseUrl("cip-fraud-service")+cipConfig.get[String]("submit-url")

  lazy val nrsRetries: List[FiniteDuration] =
    Retrying.fibonacciDelays(getFiniteDuration(nrsConfig, "initialDelay"), nrsConfig.get[Int]("numberOfRetries"))

  private final def getFiniteDuration(config: Configuration, path: String): FiniteDuration = {
    val string = config.get[String](path)

    Duration.create(string) match {
      case f: FiniteDuration => f
      case _                 => throw new RuntimeException(s"Not a finite duration '$string' for $path")
    }
  }
}
