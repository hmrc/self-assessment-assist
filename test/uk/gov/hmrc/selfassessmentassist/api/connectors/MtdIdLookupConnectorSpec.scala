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

package uk.gov.hmrc.selfassessmentassist.api.connectors

import uk.gov.hmrc.selfassessmentassist.api.models.errors.InternalError
import uk.gov.hmrc.selfassessmentassist.support.{ConnectorSpec, MockAppConfig}
import uk.gov.hmrc.selfassessmentassist.v1.mocks.MockHttpClient

import scala.concurrent.Future

class MtdIdLookupConnectorSpec extends ConnectorSpec {

  class Test extends MockHttpClient with MockAppConfig {

    val connector = new MtdIdLookupConnector(
      http = mockHttpClient,
      appConfig = mockAppConfig
    )

    MockedAppConfig.mtdIdBaseUrl returns baseUrl
  }

  val nino: String  = "test-nino"
  val mtdId: String = "test-mtdId"

  "getMtdId" should {
    "return an MtdId" when {
      "the http client returns a mtd id" in new Test {
        MockedHttpClient
          .get[MtdIdLookupOutcome](
            url = s"$baseUrl/mtd-identifier-lookup/nino/$nino",
            config = dummyHeaderCarrierConfig
          )
          .returns(Future.successful(Right(mtdId)))

        val result: MtdIdLookupOutcome = await(connector.getMtdId(nino))
        result shouldBe Right(mtdId)
      }
    }

    "return a DownstreamError" when {
      "the http client returns a DownstreamError" in new Test {
        MockedHttpClient
          .get[MtdIdLookupOutcome](
            url = s"$baseUrl/mtd-identifier-lookup/nino/$nino",
            config = dummyHeaderCarrierConfig
          )
          .returns(Future.successful(Left(InternalError)))

        val result: MtdIdLookupOutcome = await(connector.getMtdId(nino))
        result shouldBe Left(InternalError)
      }
    }
  }

}
