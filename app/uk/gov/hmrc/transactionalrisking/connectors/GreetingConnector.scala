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

package uk.gov.hmrc.transactionalrisking.connectors

import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.transactionalrisking.models.Greeting

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

/**
 * This is an example showing how to use the HMRC libraries to "connect" and consume "stubs" downstream services.
 *
 * @param http is the HMRC helper acting as a HTTP client and providing HTTP methods such as GET, POST, DELETE, etc.
 * @param services is the HMRC helper providing configuration of micro services
 */
@Singleton
class GreetingConnector @Inject()(http: HttpClient, services: ServicesConfig)(implicit ec: ExecutionContext) {

  // Get the (protocol, hostname, port) of the downstream service to be consume (e.g. any of the "stubs")
  val baseUrl = s"""${services.baseUrl("stubs")}"""

  /**
   * This is an example showing how to get some resource from a downstream service
   *
   * @param hc is the HMRC helper able to convey important HTTP headers (such as those relating to security)
   * @return
   */
  def getGreeting()(implicit hc: HeaderCarrier): Future[Greeting] = {
    http.GET[Greeting](url = s"$baseUrl/greet")
  }
}
