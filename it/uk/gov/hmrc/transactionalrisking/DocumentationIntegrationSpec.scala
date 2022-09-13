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

package uk.gov.hmrc.transactionalrisking

import org.scalatest.Inspectors.forAll
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient

class DocumentationIntegrationSpec
  extends AnyWordSpec
    with Matchers
    with ScalaFutures
    with IntegrationPatience
    with GuiceOneServerPerSuite {

  private val wsClient = app.injector.instanceOf[WSClient]
  private val baseUrl = s"http://localhost:$port"

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(
        "metrics.enabled" -> false,
        "auditing.enabled" -> false)
      .build()

  "a request for an OpenAPI Spec" should {
    "respond with a 200 status" when {
      val knownVersions = Seq("1.0")
      forAll(knownVersions) { knownVersion: String =>
        "the version is known" in {
          val response =
            wsClient
              .url(s"$baseUrl/api/conf/$knownVersion/application.yaml")
              .get()
              .futureValue
          response.status shouldBe 200
          response.contentType shouldBe "application/octet-stream"
          response.body should include("openapi: \"3.0.3\"")
          response.body should include(s"version: $knownVersion")
        }
      }
    }
    "respond with a 404 status" when {
      "the version is unknown" in {
        val response =
          wsClient
            .url(s"$baseUrl/api/conf/4.4.42/application.yaml")
            .get()
            .futureValue
        response.status shouldBe 404
      }
    }
  }
}

