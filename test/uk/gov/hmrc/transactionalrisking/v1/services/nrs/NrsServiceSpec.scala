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
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.transactionalrisking.support.ServiceSpec
import uk.gov.hmrc.transactionalrisking.utils.{DateUtils, HashUtil}
import uk.gov.hmrc.transactionalrisking.v1.controllers.UserRequest
import uk.gov.hmrc.transactionalrisking.v1.models.auth.UserDetails
import uk.gov.hmrc.transactionalrisking.v1.services.nrs.models.request._
import uk.gov.hmrc.transactionalrisking.v1.services.nrs.models.response.{NrsFailure, NrsResponse}

import java.time.OffsetDateTime
import scala.concurrent.Future

class NrsServiceSpec extends ServiceSpec {

  private val nrsId = "a5894863-9cd7-4d0d-9eee-301ae79cbae6"
  private val timestamp: OffsetDateTime = OffsetDateTime.parse("2018-04-07T12:13:25.156Z")
  private val formattedDate: String = timestamp.format(DateUtils.isoInstantDatePattern)

  class Test extends MockNrsConnector {

    private val hasUtil = app.injector.instanceOf[HashUtil]

    implicit val userRequest: UserRequest[_] =
      UserRequest(
        userDetails =
          UserDetails(
            userType = AffinityGroup.Individual,
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

    val service = new NrsService(mockNrsConnector, hasUtil)
  }

  private val nrsSubmissionAssistReportGenerated: NrsSubmission =
    NrsSubmission(
      payload = "eyJyZXBvcnRJZCI6IjEyMzQ1In0=",
      metadata = Metadata(
        businessId = "saa",
        notableEvent = "saa-report-generated",
        payloadContentType = "application/json",
        payloadSha256Checksum = "eb6b96e57239c4605bd34ab5ea3dec01707ae960794d647ca50818efcebe6429",
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
            reportId = "12345"
          )
      )
    )


//  "service using report generated" when {
//    "service call successful" must {
//      "return the expected result" in new Test {
//
//        MockNrsConnector.submitNrs(nrsSubmissionAssistReportGenerated, reportId)
//          .returns(Future.successful(Right(NrsResponse(nrsId))))
//
//        MockedHashUtil.encode(generateReportBodyRequestString).returns(encodedString)
//        MockedHashUtil.getHash(generateReportBodyRequestString).returns(checksum)
//
//        await(service.submit(selfAssessmentSubmission, timestamp, AssistReportGenerated)) shouldBe Some(NrsResponse("a5894863-9cd7-4d0d-9eee-301ae79cbae6"))
//      }
//    }
//  }
//
//  "service call unsuccessful report generated" must {
//    "map 4xx errors correctly" in new Test {
//
//      MockedHashUtil.encode(generateReportBodyRequestString).returns(encodedString)
//      MockedHashUtil.getHash(generateReportBodyRequestString).returns(checksum)
//
//      MockNrsConnector.submitNrs(nrsSubmissionAssistReportGenerated, reportId)
//        .returns(Future.successful(Left(NrsFailure.ExceptionThrown)))
//
//      await(service.submit(selfAssessmentSubmission, timestamp, AssistReportGenerated)) shouldBe None
//    }
//  }


  private val acknowledgeRdsReport = AcknowledgeReportId("12345")

  private val expectedAcknowledgePayload: NrsSubmission =
    NrsSubmission(
      payload = "eyJyZXBvcnRJZCI6IjEyMzQ1In0=",
      metadata = Metadata(
        businessId = "saa",
        notableEvent = AssistReportAcknowledged.value,
        payloadContentType = "application/json",
        payloadSha256Checksum = "eb6b96e57239c4605bd34ab5ea3dec01707ae960794d647ca50818efcebe6429",
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
            reportId = "12345"
          )
      )
    )

  "service using acknowledged generated" when {

    "service call successful" must {

      "return the expected result" in new Test {

        MockNrsConnector.submitNrs(expectedPayload = expectedAcknowledgePayload)
          .returns(Future.successful(Right(NrsResponse(nrsId))))

        await(service.submit(acknowledgeRdsReport, timestamp, AssistReportAcknowledged)) shouldBe Some(NrsResponse("a5894863-9cd7-4d0d-9eee-301ae79cbae6"))
      }
    }
  }

  "service call unsuccessful acknowledged generated" must {

    "map 4xx errors correctly" in new Test {

      MockNrsConnector.submitNrs(expectedPayload = expectedAcknowledgePayload)
        .returns(Future.successful(Left(NrsFailure.ExceptionThrown)))

      await(service.submit(acknowledgeRdsReport, timestamp, AssistReportAcknowledged)) shouldBe None
    }
  }


}
