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

package uk.gov.hmrc.transactionalrisking.v1.services.nrs


import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.transactionalrisking.support.ServiceSpec
import uk.gov.hmrc.transactionalrisking.utils.DateUtils
import uk.gov.hmrc.transactionalrisking.v1.controllers.AuthorisedController.ninoKey
import uk.gov.hmrc.transactionalrisking.v1.controllers.UserRequest
import uk.gov.hmrc.transactionalrisking.v1.models.auth.UserDetails
import uk.gov.hmrc.transactionalrisking.v1.services.nrs.models.request._
import uk.gov.hmrc.transactionalrisking.v1.services.nrs.models.response.{NrsFailure, NrsResponse}

import java.time.OffsetDateTime
import scala.concurrent.Future

class NrsServiceSpec extends ServiceSpec {

  private val reportId = "12345"
  private val nino = "AA00000B"
  private val encodedString: String = "encodedString"
  private val checksum: String = "checksum"
  private val nrsId = "a5894863-9cd7-4d0d-9eee-301ae79cbae6"
  private val timestamp: OffsetDateTime = OffsetDateTime.parse("2018-04-07T12:13:25.156Z")
  private val formattedDate: String = timestamp.format(DateUtils.isoInstantDatePattern)
  private val newRdsReport = "AReport"
  private val taxYear = "2017-18"

  private val generateReportBodyRequest: RequestBody = RequestBody(newRdsReport, reportId)
  private val selfAssessmentSubmission: RequestData = RequestData(nino, generateReportBodyRequest)

  private val generateReportBodyRequestString = Json.toJson(generateReportBodyRequest).toString

  private val nrsSubmission: NrsSubmission =
    NrsSubmission(
      payload = encodedString,
      metadata = Metadata(
        businessId = "self-assessment-assist",
        notableEvent = "assist-report-generated",
        payloadContentType = "application/json",
        payloadSha256Checksum = checksum,
        userSubmissionTimestamp = formattedDate,
        identityData = Some(IdentityDataTestData.correctModel),
        userAuthToken = "Bearer aaaa",
        headerData = Json.toJson(Map(
          "Host" -> "localhost",
          "dummyHeader1" -> "dummyValue1",
          "dummyHeader2" -> "dummyValue2",
          "Authorization" -> "Bearer aaaa"
        )),
        searchKeys =
          SearchKeys(
            nino = ninoKey,
            taxYear = taxYear,
            reportId = "12345"
          )
      )
    )

  class Test extends MockNrsConnector with MockHashUtil {

    implicit val userRequest: UserRequest[_] =
      UserRequest(
        userDetails =
          UserDetails(
            userType = "Individual",
            agentReferenceNumber = None,
            clientID = "aClientID",
            identityData = Some(IdentityDataTestData.correctModel)
          ),
        request = FakeRequest().withHeaders(
          "Authorization" -> "Bearer aaaa",
          "dummyHeader1" -> "dummyValue1",
          "dummyHeader2" -> "dummyValue2"
        )
      )

   val service = new NrsService(mockNrsConnector, mockHashUtil)
  }

  "service" when {
    "service call successful" must {
      "return the expected result" in new Test {

        MockNrsConnector.submitNrs(nrsSubmission, reportId)
          .returns(Future.successful(Right(NrsResponse(nrsId))))

        MockedHashUtil.encode(generateReportBodyRequestString).returns(encodedString)
        MockedHashUtil.getHash(generateReportBodyRequestString).returns(checksum)

        await(service.submit(selfAssessmentSubmission, timestamp, AssistReportGenerated,taxYear)) shouldBe Some(NrsResponse("a5894863-9cd7-4d0d-9eee-301ae79cbae6"))
      }
    }
  }

  "service call unsuccessful" must {
    "map 4xx errors correctly" in new Test {

      MockedHashUtil.encode(generateReportBodyRequestString).returns(encodedString)
      MockedHashUtil.getHash(generateReportBodyRequestString).returns(checksum)

      MockNrsConnector.submitNrs(nrsSubmission, reportId)
        .returns(Future.successful(Left(NrsFailure.ExceptionThrown)))

      await(service.submit(selfAssessmentSubmission, timestamp, AssistReportGenerated,taxYear)) shouldBe None
    }
  }
}
