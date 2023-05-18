/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.selfassessmentassist.support

import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTime, DateTimeZone}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}
import play.api.{Application, Environment, Mode}

trait IntegrationBaseSpec extends IntegrationSpec with WireMockHelper with GuiceOneServerPerSuite with BeforeAndAfterEach with BeforeAndAfterAll {

  val mockHost: String = WireMockHelper.host
  val mockPort: String = WireMockHelper.wireMockPort.toString

  lazy val client: WSClient = app.injector.instanceOf[WSClient]

  def servicesConfig: Map[String, Any] = Map(
    "microservice.services.ifs.host"               -> mockHost,
    "microservice.services.ifs.port"               -> mockPort,
    "microservice.services.mtd-id-lookup.host"     -> mockHost,
    "microservice.services.mtd-id-lookup.port"     -> mockPort,
    "microservice.services.auth.host"              -> mockHost,
    "microservice.services.auth.port"              -> mockPort,
    "auditing.consumer.baseUri.port"               -> mockPort,
    "microservice.services.rds.port"               -> mockPort,
    "microservice.services.non-repudiation.port"   -> mockPort,
    "microservice.services.cip-fraud-service.port" -> mockPort,
    "feature-switch.version-1.enabled"             -> "true"
  )

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .in(Environment.simple(mode = Mode.Dev))
    .configure(servicesConfig)
    .build()

  override def beforeAll(): Unit = {
    super.beforeAll()
    startWireMock()
  }

  override def afterAll(): Unit = {
    stopWireMock()
    super.afterAll()
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    resetWireMock()
  }

  def buildRequest(path: String): WSRequest = client.url(s"http://localhost:$port$path").withFollowRedirects(false)

  def document(response: WSResponse): JsValue = Json.parse(response.body)

  def getCurrentTaxYear: String = {
    val currentDate = DateTime.now(DateTimeZone.UTC)

    val taxYearStartDate: DateTime = DateTime.parse(
      currentDate.getYear + "-04-06",
      DateTimeFormat.forPattern("yyyy-MM-dd")
    )

    def fromDesIntToString(taxYear: Int): String =
      (taxYear - 1) + "-" + taxYear.toString.drop(2)

    if (currentDate.isBefore(taxYearStartDate)) {
      fromDesIntToString(currentDate.getYear)
    } else {
      fromDesIntToString(currentDate.getYear + 1)
    }
  }

}
