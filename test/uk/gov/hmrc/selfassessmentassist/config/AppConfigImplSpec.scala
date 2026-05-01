/*
 * Copyright 2026 HM Revenue & Customs
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

import com.typesafe.config.{Config, ConfigFactory}
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.selfassessmentassist.api.models.auth.AuthCredential
import uk.gov.hmrc.selfassessmentassist.support.UnitSpec

class AppConfigImplSpec extends UnitSpec {

  private def appConfig(config: String, withSupportingAgentEndpointsKey: Boolean = true): AppConfigImpl = {
    val base: Config = ConfigFactory.parseString(config).withFallback(ConfigFactory.load())
    val conf: Config = if (withSupportingAgentEndpointsKey) base else base.withoutPath("api.supporting-agent-endpoints")

    val configuration: Configuration   = Configuration(conf)
    val servicesConfig: ServicesConfig = new ServicesConfig(configuration)

    new AppConfigImpl(servicesConfig, configuration)
  }

  "AppConfigImpl" when {
    ".endpointAllowsSupportingAgents" should {
      "return false when the config key is present but the endpoint is not configured" in {
        appConfig("").endpointAllowsSupportingAgents("unknown-endpoint") shouldBe false
      }

      "return false when supporting-agent-endpoints config key is absent" in {
        appConfig("", withSupportingAgentEndpointsKey = false).endpointAllowsSupportingAgents("any-endpoint") shouldBe false
      }

      "return the correct value when the config key is present and the endpoints are configured" in {
        val config =
          """
            |  api.supporting-agent-endpoints {
            |     acknowledge-report = false
            |     generate-report = true
            |  }
          """.stripMargin

        appConfig(config).endpointAllowsSupportingAgents("acknowledge-report") shouldBe false
        appConfig(config).endpointAllowsSupportingAgents("generate-report") shouldBe true
      }
    }

    ".rdsAuthCredential" should {
      "return an AuthCredential with values from config" in {
        appConfig("").rdsAuthCredential shouldBe AuthCredential(
          client_id = "stub-client-id",
          client_secret = "stub-secret-id",
          grant_type = "client_credentials"
        )
      }
    }

    ".nrsRetries" should {
      "throw RuntimeException if duration is not finite" in {
        val config =
          """
            |  microservice.services.non-repudiation {
            |     initialDelay = "Inf"
            |  }
          """.stripMargin

        intercept[RuntimeException] {
          appConfig(config).nrsRetries
        }.getMessage shouldBe "Not a finite duration 'Inf' for initialDelay"
      }
    }
  }

}
