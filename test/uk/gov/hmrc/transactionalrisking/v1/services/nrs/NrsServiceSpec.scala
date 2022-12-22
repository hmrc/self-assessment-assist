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
import uk.gov.hmrc.transactionalrisking.v1.models.domain.{AssessmentReport, Link, Risk}
import uk.gov.hmrc.transactionalrisking.v1.services.nrs.models.request._
import uk.gov.hmrc.transactionalrisking.v1.services.nrs.models.response.{NrsFailure, NrsResponse}

import java.time.OffsetDateTime
import java.util.UUID
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

  private val rdsReport: AssessmentReport = AssessmentReport(
    reportId = UUID.fromString("db741dff-4054-478e-88d2-5993e925c7ab"),
    risks = Seq(
      Risk(
        title = "Turnover and cost of sales",
        body = "Your cost of sales is greater than income",
        action = "Please read our guidance",
        links = Seq(Link(title = "Our guidance", url = "https://www.gov.uk/expenses-if-youre-self-employed")),
        path = "general/total_declared_turnover"
      )
    ),
    nino = "nino",
    taxYear = "2021-2022",
    calculationId = UUID.fromString("99d758f6-c4be-4339-804e-f79cf0610d4f"),
    rdsCorrelationId = "e43264c5-5301-4ece-b3d3-1e8a8dd93b4b"
  )

  private val expectedReportPayload: NrsSubmission =
    NrsSubmission(
      payload = "eyJyZXBvcnRJZCI6ImRiNzQxZGZmLTQwNTQtNDc4ZS04OGQyLTU5OTNlOTI1YzdhYiIsIm1lc3NhZ2VzIjpbeyJ0aXRsZSI6IlR1cm5vdmVyIGFuZCBjb3N0IG9mIHNhbGVzIiwiYm9keSI6IllvdXIgY29zdCBvZiBzYWxlcyBpcyBncmVhdGVyIHRoYW4gaW5jb21lIiwiYWN0aW9uIjoiUGxlYXNlIHJlYWQgb3VyIGd1aWRhbmNlIiwibGlua3MiOlt7InRpdGxlIjoiT3VyIGd1aWRhbmNlIiwidXJsIjoiaHR0cHM6Ly93d3cuZ292LnVrL2V4cGVuc2VzLWlmLXlvdXJlLXNlbGYtZW1wbG95ZWQifV0sInBhdGgiOiJnZW5lcmFsL3RvdGFsX2RlY2xhcmVkX3R1cm5vdmVyIn1dLCJuaW5vIjoibmlubyIsInRheFllYXIiOiIyMDIxLTIwMjIiLCJjYWxjdWxhdGlvbklkIjoiOTlkNzU4ZjYtYzRiZS00MzM5LTgwNGUtZjc5Y2YwNjEwZDRmIiwiY29ycmVsYXRpb25JZCI6ImU0MzI2NGM1LTUzMDEtNGVjZS1iM2QzLTFlOGE4ZGQ5M2I0YiJ9",
      metadata = Metadata(
        businessId = "saa",
        notableEvent = "saa-report-generated",
        payloadContentType = "application/json",
        payloadSha256Checksum = "acdf5c0add9e434375e81797ad21fd409bc55f6d4f264d7aa302ca1ef4a01058",
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
            reportId = "db741dff-4054-478e-88d2-5993e925c7ab"
          )
      )
    )


  "service using report generated" when {

    "service call successful" must {
      "return the expected result" in new Test {

        MockNrsConnector.submitNrs(expectedPayload = expectedReportPayload)
          .returns(Future.successful(Right(NrsResponse(nrsId))))

        await(service.submit(rdsReport, timestamp)) shouldBe Some(NrsResponse("a5894863-9cd7-4d0d-9eee-301ae79cbae6"))
      }
    }
  }

  "service call unsuccessful report generated" must {
    "map 4xx errors correctly" in new Test {

      MockNrsConnector.submitNrs(expectedPayload = expectedReportPayload)
        .returns(Future.successful(Left(NrsFailure.ExceptionThrown)))

      await(service.submit(rdsReport, timestamp)) shouldBe None
    }
  }


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

        await(service.submit(acknowledgeRdsReport, timestamp)) shouldBe Some(NrsResponse("a5894863-9cd7-4d0d-9eee-301ae79cbae6"))
      }
    }
  }

  "service call unsuccessful acknowledged generated" must {

    "map 4xx errors correctly" in new Test {

      MockNrsConnector.submitNrs(expectedPayload = expectedAcknowledgePayload)
        .returns(Future.successful(Left(NrsFailure.ExceptionThrown)))

      await(service.submit(acknowledgeRdsReport, timestamp)) shouldBe None
    }
  }


}
