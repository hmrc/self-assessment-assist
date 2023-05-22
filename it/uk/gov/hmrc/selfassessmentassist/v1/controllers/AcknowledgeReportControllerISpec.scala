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

package uk.gov.hmrc.selfassessmentassist.v1.controllers

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status.{BAD_REQUEST, NO_CONTENT}
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{WSRequest, WSResponse}
import play.api.test.Helpers.{ACCEPT, AUTHORIZATION, await, defaultAwaitTimeout}
import uk.gov.hmrc.selfassessmentassist.stubs._
import uk.gov.hmrc.selfassessmentassist.support.IntegrationBaseSpec
import uk.gov.hmrc.selfassessmentassist.api.models.errors.{FormatReportIdError, MtdError, NinoFormatError}

import scala.collection.Seq

class AcknowledgeReportControllerISpec extends IntegrationBaseSpec {

  "Calling the acknowledge report endpoint" should {
    "return a 204 status code" when {
      "any valid request is made" in new Test {

        override def setupStubs(): StubMapping = {
          MtdIdLookupStub.ninoFound(nino)
          AuthStub.authorised()
          RdsStub.acknowledge(acknowledgeUrl)
          IfsServiceStub.submit(ifsSubmitUrl)
          NrsStub.submit(nrsSubmitUrl)
        }

        val response: WSResponse = await(request().post(emptyJson))
        response.status shouldBe NO_CONTENT
      }
    }

    "return error according to spec" when {
      "validation error" when {
        def validationErrorTest(requestNino: String, expectedReportId: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
          s"validation fails with ${expectedBody.code} error" in new Test {
            override def nino: String     = requestNino
            override def reportId: String = expectedReportId

            override def setupStubs(): StubMapping = {
              AuthStub.authorised()
              MtdIdLookupStub.ninoFound(nino)
            }

            val response: WSResponse = await(request().post(emptyJson))
            response.status shouldBe expectedStatus
            response.json shouldBe Json.arr(expectedBody)
            response.header("Content-Type") shouldBe Some("application/json")
          }
        }

        val input = Seq(
          ("AA1256A", "f2fb30e5-4ab6-4a29-b3c1-c00000000001", BAD_REQUEST, NinoFormatError),
          ("AA123456A", "fb30e5-4ab6-4a29-b3c1-c00000000", BAD_REQUEST, FormatReportIdError)
        )

        input.foreach(args => (validationErrorTest _).tupled(args))
      }

    }
  }

  private trait Test {

    def nino: String             = "AA123456A"
    def reportId: String         = CommonTestData.simpleCalculationId.toString
    val acknowledgeUrl: String   = "/rds/assessments/self-assessment-assist/acknowledge"
    val ifsSubmitUrl: String     = "/interaction-data/store-interactions"
    val nrsSubmitUrl: String     = "/submission"
    val rdsCorrelationId: String = CommonTestData.correlationId
    val emptyJson: JsValue       = Json.parse("""{}""")

    def setupStubs(): StubMapping

    val uri = s"/reports/acknowledge/$nino/$reportId/$rdsCorrelationId"

    def request(): WSRequest = {
      setupStubs()
      buildRequest(uri)
        .withHttpHeaders(
          (ACCEPT, "application/vnd.hmrc.1.0+json"),
          (AUTHORIZATION, "Bearer 123")
        )
    }

  }

}
