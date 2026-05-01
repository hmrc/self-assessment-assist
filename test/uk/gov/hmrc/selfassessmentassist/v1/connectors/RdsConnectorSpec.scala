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

package uk.gov.hmrc.selfassessmentassist.v1.connectors

import play.api.libs.json.{JsArray, JsNumber, JsObject, JsValue, Json}
import play.api.libs.ws.WSBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.{BadRequestException, HttpException, HttpResponse, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.selfassessmentassist.api.TestData.CommonTestData.*
import uk.gov.hmrc.selfassessmentassist.api.models.auth.RdsAuthCredentials
import uk.gov.hmrc.selfassessmentassist.api.models.errors.*
import uk.gov.hmrc.selfassessmentassist.api.models.outcomes.ResponseWrapper
import uk.gov.hmrc.selfassessmentassist.support.{ConnectorSpec, MockAppConfig}
import uk.gov.hmrc.selfassessmentassist.v1.mocks.MockHttpClient
import uk.gov.hmrc.selfassessmentassist.v1.models.response.rds.RdsAssessmentReport
import uk.gov.hmrc.selfassessmentassist.v1.services.ServiceOutcome
import uk.gov.hmrc.selfassessmentassist.v1.services.testData.RdsTestData.{rdsAcknowledgementRequest, rdsRequest}
import uk.gov.hmrc.selfassessmentassist.v1.utils.StubResource.{loadAckResponseTemplate, loadSubmitResponseTemplate}

import java.net.URL
import java.util.UUID
import scala.concurrent.Future

class RdsConnectorSpec extends ConnectorSpec with MockAppConfig with MockHttpClient {

  private val submitBaseUrl: String  = s"$baseUrl/rds/assessments/self-assessment-assist"
  private val acknowledgeUrl: String = s"$baseUrl/rds/assessments/self-assessment-assist/acknowledge"
  private val submitUrl: URL         = url"$submitBaseUrl"
  private val ackUrl: URL            = url"$acknowledgeUrl"

  private val rdsAuthCredentials: RdsAuthCredentials = RdsAuthCredentials(UUID.randomUUID().toString, "bearer", 3600)

  private val expectedHeaders: Seq[(String, String)] = Seq("Authorization" -> s"Bearer ${rdsAuthCredentials.access_token}")

  private val generateRequestJson: JsValue    = Json.toJson(rdsRequest)
  private val acknowledgeRequestJson: JsValue = Json.toJson(rdsAcknowledgementRequest)

  private trait Test {
    MockedAppConfig.rdsBaseUrlForSubmit returns submitBaseUrl
    MockedAppConfig.rdsBaseUrlForAcknowledge returns acknowledgeUrl

    val connector: RdsConnector = new RdsConnector(mockHttpClient, mockAppConfig)

    private def makeHttpResponse(body: Option[String], status: Int): Future[HttpResponse] =
      body match {
        case Some(data) => Future.successful(HttpResponse(status, data))
        case None       => Future.successful(HttpResponse(status))
      }

    def mockRdsGenerateReportCall(body: Option[String] = None, status: Int = CREATED): Unit =
      MockedHttpClient
        .post(submitUrl, generateRequestJson, expectedHeaders, useProxy = true)
        .returns(makeHttpResponse(body, status))

    def mockRdsAcknowledgeReportCall(body: Option[String] = None, status: Int = CREATED): Unit =
      MockedHttpClient
        .post(ackUrl, acknowledgeRequestJson, expectedHeaders, useProxy = true)
        .returns(makeHttpResponse(body, status))

    def mockRdsGenerateException(ex: Throwable): Unit =
      MockedHttpClient
        .post(submitUrl, generateRequestJson, expectedHeaders, useProxy = true)
        .returns(Future.failed(ex))

    def mockRdsAcknowledgeException(ex: Throwable): Unit =
      MockedHttpClient
        .post(ackUrl, acknowledgeRequestJson, expectedHeaders, useProxy = true)
        .returns(Future.failed(ex))

  }

  "RdsConnector" when {
    ".submit" should {
      "return the success response if successful" in new Test {
        mockRdsGenerateReportCall(Some(rdsSubmissionReportJson.toString), CREATED)

        val result: ServiceOutcome[RdsAssessmentReport] = await(connector.submit(rdsRequest, Some(rdsAuthCredentials)))

        result shouldBe Right(ResponseWrapper(correlationId, rdsNewSubmissionReport))
      }

      "fail when the bearer token is invalid" in new Test {
        mockRdsGenerateReportCall(status = UNAUTHORIZED)

        val result: ServiceOutcome[RdsAssessmentReport] = await(connector.submit(rdsRequest, Some(rdsAuthCredentials)))

        result shouldBe Left(ErrorWrapper(correlationId, ForbiddenDownstreamError))
      }

      "return the feedback, if RDS returns http status 201 and feedback with responseCode 201" in new Test {
        mockRdsGenerateReportCall(Some(rdsSubmissionReportJson.toString), status = CREATED)

        val result: ServiceOutcome[RdsAssessmentReport] = await(connector.submit(rdsRequest, Some(rdsAuthCredentials)))

        result shouldBe Right(ResponseWrapper(correlationId, rdsNewSubmissionReport))
      }

      "return the empty feedback, if RDS returns http status 201 and no feedback with responseCode 204" in new Test {
        val rdsReportJson: JsValue = loadSubmitResponseTemplate(calculationIdWithNoFeedback.toString, simpleReportId.toString, simpleRDSCorrelationId)

        mockRdsGenerateReportCall(Some(rdsReportJson.toString), status = CREATED)

        val result: ServiceOutcome[RdsAssessmentReport] = await(connector.submit(rdsRequest, Some(rdsAuthCredentials)))

        result shouldBe Left(ErrorWrapper(correlationId, NoAssessmentFeedbackFromRDS))
      }

      "return MatchingResourcesNotFoundError, if RDS returns http status 201 and no calculationId found with responseCode 404" in new Test {
        val rdsReportJson: JsValue = loadSubmitResponseTemplate(noCalculationFound.toString, simpleReportId.toString, simpleRDSCorrelationId)

        mockRdsGenerateReportCall(Some(rdsReportJson.toString), status = CREATED)

        val result: ServiceOutcome[RdsAssessmentReport] = await(connector.submit(rdsRequest, Some(rdsAuthCredentials)))

        result shouldBe Left(
          ErrorWrapper(correlationId, MatchingCalculationIDNotFoundError, Some(Seq(MtdError("404", "No feedback applicable", NOT_FOUND))))
        )
      }

      "return Internal Server Error, if RDS returns http status 400" in new Test {
        mockRdsGenerateReportCall(status = BAD_REQUEST)

        val result: ServiceOutcome[RdsAssessmentReport] = await(connector.submit(rdsRequest, Some(rdsAuthCredentials)))

        result shouldBe Left(ErrorWrapper(correlationId, InternalError))
      }

      "return Internal Server Error, if RDS is (unavailable) http status code 404" in new Test {
        mockRdsGenerateReportCall(status = NOT_FOUND)

        val result: ServiceOutcome[RdsAssessmentReport] = await(connector.submit(rdsRequest, Some(rdsAuthCredentials)))

        result shouldBe Left(ErrorWrapper(correlationId, InternalError))
      }

      "return Internal Server Error, if RDS returns http status code 500" in new Test {
        mockRdsGenerateReportCall(status = INTERNAL_SERVER_ERROR)

        val result: ServiceOutcome[RdsAssessmentReport] = await(connector.submit(rdsRequest, Some(rdsAuthCredentials)))

        result shouldBe Left(ErrorWrapper(correlationId, InternalError))
      }

      "return Internal Server Error, if RDS fails with 503" in new Test {
        mockRdsGenerateReportCall(status = SERVICE_UNAVAILABLE)

        val result: ServiceOutcome[RdsAssessmentReport] = await(connector.submit(rdsRequest, Some(rdsAuthCredentials)))

        result shouldBe Left(ErrorWrapper(correlationId, InternalError))
      }

      "return Internal Server Error, if RDS request times out" in new Test {
        mockRdsGenerateReportCall(status = REQUEST_TIMEOUT)

        val result: ServiceOutcome[RdsAssessmentReport] = await(connector.submit(rdsRequest, Some(rdsAuthCredentials)))

        result shouldBe Left(ErrorWrapper(correlationId, InternalError))
      }

      "return InternalError when submit returns http 201 but unexpected responseCode" in new Test {
        val base: JsObject =
          loadSubmitResponseTemplate(
            simpleCalculationId.toString,
            simpleReportId.toString,
            simpleRDSCorrelationId
          ).as[JsObject]

        val patchedOutputs: collection.IndexedSeq[JsObject] = (base \ "outputs").as[JsArray].value.map { js =>
          val o = js.as[JsObject]
          (o \ "name").asOpt[String] match {
            case Some("responseCode") => o + ("value" -> JsNumber(INTERNAL_SERVER_ERROR))
            case _                    => o
          }
        }

        val patched: JsObject = base + ("outputs" -> JsArray(patchedOutputs))

        mockRdsGenerateReportCall(Some(patched.toString), status = CREATED)

        val result: ServiceOutcome[RdsAssessmentReport] = await(connector.submit(rdsRequest, Some(rdsAuthCredentials)))

        result shouldBe Left(
          ErrorWrapper(
            correlationId,
            InternalError,
            Some(
              Seq(
                MtdError(
                  InternalError.code,
                  "unexpected response from downstream",
                  INTERNAL_SERVER_ERROR
                )
              )
            )
          )
        )
      }

      "return InternalError when submit returns invalid JSON" in new Test {
        mockRdsGenerateReportCall(
          body = Some("""{ "invalid": "json" }"""),
          status = CREATED
        )

        val result: ServiceOutcome[RdsAssessmentReport] = await(connector.submit(rdsRequest, Some(rdsAuthCredentials)))

        result shouldBe Left(
          ErrorWrapper(
            correlationId,
            InternalError,
            Some(
              Seq(
                MtdError(
                  InternalError.code,
                  "unexpected response from downstream",
                  INTERNAL_SERVER_ERROR
                )
              )
            )
          )
        )
      }

      "return InternalError when submit returns unexpected status" in new Test {
        mockRdsGenerateReportCall(status = IM_A_TEAPOT)

        val result: ServiceOutcome[RdsAssessmentReport] = await(connector.submit(rdsRequest, Some(rdsAuthCredentials)))

        result shouldBe Left(ErrorWrapper(correlationId, InternalError))
      }

      "return InternalError when BadRequestException is returned" in new Test {
        mockRdsGenerateException(new BadRequestException("test"))

        val result: ServiceOutcome[RdsAssessmentReport] = await(connector.submit(rdsRequest, Some(rdsAuthCredentials)))

        result shouldBe Left(ErrorWrapper(correlationId, InternalError))
      }

      "return InternalError when UpstreamErrorResponse is returned" in new Test {
        mockRdsGenerateException(UpstreamErrorResponse("test", BAD_GATEWAY, BAD_GATEWAY))

        val result: ServiceOutcome[RdsAssessmentReport] = await(connector.submit(rdsRequest, Some(rdsAuthCredentials)))

        result shouldBe Left(ErrorWrapper(correlationId, InternalError))
      }

      "return ServiceUnavailableError when HttpException is returned" in new Test {
        mockRdsGenerateException(new HttpException("test", SERVICE_UNAVAILABLE))

        val result: ServiceOutcome[RdsAssessmentReport] = await(connector.submit(rdsRequest, Some(rdsAuthCredentials)))

        result shouldBe Left(ErrorWrapper(correlationId, ServiceUnavailableError))
      }

      "return ServiceUnavailableError when unknown Throwable is returned" in new Test {
        mockRdsGenerateException(new RuntimeException("test"))

        val result: ServiceOutcome[RdsAssessmentReport] = await(connector.submit(rdsRequest, Some(rdsAuthCredentials)))

        result shouldBe Left(ErrorWrapper(correlationId, ServiceUnavailableError))
      }
    }

    ".acknowledgeRds" should {
      "return the success response if successful" in new Test {
        val rdsAssessmentAckJson: JsValue = loadAckResponseTemplate(simpleReportId.toString, nino = simpleNino, responseCode = 202)

        mockRdsAcknowledgeReportCall(Some(rdsAssessmentAckJson.toString), status = CREATED)

        val result: ServiceOutcome[RdsAssessmentReport] = await(connector.acknowledgeRds(rdsAcknowledgementRequest, Some(rdsAuthCredentials)))

        result shouldBe Right(ResponseWrapper(correlationId, rdsAssessmentAckJson.as[RdsAssessmentReport]))
      }

      "fail when the bearer token is invalid" in new Test {
        mockRdsAcknowledgeReportCall(status = UNAUTHORIZED)

        val result: ServiceOutcome[RdsAssessmentReport] = await(connector.acknowledgeRds(rdsAcknowledgementRequest, Some(rdsAuthCredentials)))

        result shouldBe Left(ErrorWrapper(correlationId, ForbiddenDownstreamError))
      }

      "return no content, if RDS returns http status 201 and with responsecode 202" in new Test {
        val rdsAssessmentAckJson: JsValue = loadAckResponseTemplate(simpleReportId.toString, nino = simpleNino, responseCode = 202)

        mockRdsAcknowledgeReportCall(Some(rdsAssessmentAckJson.toString), status = CREATED)

        val result: ServiceOutcome[RdsAssessmentReport] = await(connector.acknowledgeRds(rdsAcknowledgementRequest, Some(rdsAuthCredentials)))

        result shouldBe Right(ResponseWrapper(correlationId, simpleAcknowledgeNewRdsAssessmentReport))
      }

      "return ForbiddenRDSCorrelationIdError, if RDS returns http status 201 with responsecode 401 for reportid  and correlationId combination " in new Test {
        val rdsAssessmentAckJson: JsValue = loadAckResponseTemplate(simpleReportId.toString, nino = simpleNino, responseCode = 401)

        mockRdsAcknowledgeReportCall(Some(rdsAssessmentAckJson.toString), status = CREATED)

        val result: ServiceOutcome[RdsAssessmentReport] = await(connector.acknowledgeRds(rdsAcknowledgementRequest, Some(rdsAuthCredentials)))

        result shouldBe Left(ErrorWrapper(correlationId, ForbiddenRDSCorrelationIdError, None))
      }

      "return Internal Server Error, if RDS returns http status 400" in new Test {
        mockRdsAcknowledgeReportCall(status = BAD_REQUEST)

        val result: ServiceOutcome[RdsAssessmentReport] = await(connector.acknowledgeRds(rdsAcknowledgementRequest, Some(rdsAuthCredentials)))

        result shouldBe Left(ErrorWrapper(correlationId, InternalError))
      }

      "return Internal Server Error, if RDS is (unavailable) http status code 404" in new Test {
        mockRdsAcknowledgeReportCall(status = NOT_FOUND)

        val result: ServiceOutcome[RdsAssessmentReport] = await(connector.acknowledgeRds(rdsAcknowledgementRequest, Some(rdsAuthCredentials)))

        result shouldBe Left(ErrorWrapper(correlationId, InternalError))
      }

      "return Internal Server Error, if RDS fails with 503" in new Test {
        mockRdsAcknowledgeReportCall(status = SERVICE_UNAVAILABLE)
        val feedbackReport: ServiceOutcome[RdsAssessmentReport] = await(connector.acknowledgeRds(rdsAcknowledgementRequest, Some(rdsAuthCredentials)))
        feedbackReport shouldBe Left(ErrorWrapper(correlationId, InternalError))
      }

      "return Internal Server Error, if RDS request times out" in new Test {
        mockRdsAcknowledgeReportCall(status = REQUEST_TIMEOUT)

        val result: ServiceOutcome[RdsAssessmentReport] = await(connector.acknowledgeRds(rdsAcknowledgementRequest, Some(rdsAuthCredentials)))

        result shouldBe Left(ErrorWrapper(correlationId, InternalError))
      }

      "return InternalError when acknowledge returns invalid JSON" in new Test {
        mockRdsAcknowledgeReportCall(body = Some("""{ "invalid": "json" }"""), status = CREATED)

        val result: ServiceOutcome[RdsAssessmentReport] = await(connector.acknowledgeRds(rdsAcknowledgementRequest, Some(rdsAuthCredentials)))

        result shouldBe Left(
          ErrorWrapper(
            correlationId,
            InternalError,
            Some(
              Seq(
                MtdError(
                  InternalError.code,
                  "unexpected response from downstream",
                  INTERNAL_SERVER_ERROR
                )
              )
            )
          )
        )
      }

      "return InternalError when acknowledge returns http 201 but unexpected responseCode" in new Test {
        val base: JsObject =
          loadAckResponseTemplate(simpleReportId.toString, nino = simpleNino, responseCode = 202).as[JsObject]

        val patchedOutputs: collection.IndexedSeq[JsObject] = (base \ "outputs").as[JsArray].value.map { js =>
          val o = js.as[JsObject]
          (o \ "name").asOpt[String] match {
            case Some("responseCode") => o + ("value" -> JsNumber(INTERNAL_SERVER_ERROR))
            case _                    => o
          }
        }

        val patched: JsObject = base + ("outputs" -> JsArray(patchedOutputs))

        mockRdsAcknowledgeReportCall(Some(patched.toString), status = CREATED)

        val result: ServiceOutcome[RdsAssessmentReport] = await(connector.acknowledgeRds(rdsAcknowledgementRequest, Some(rdsAuthCredentials)))

        result shouldBe Left(
          ErrorWrapper(
            correlationId,
            InternalError,
            Some(
              Seq(
                MtdError(
                  InternalError.code,
                  "unexpected response from downstream",
                  INTERNAL_SERVER_ERROR
                )
              )
            )
          )
        )
      }

      "return InternalError when BadRequestException is returned" in new Test {
        mockRdsAcknowledgeException(new BadRequestException("test"))

        val result: ServiceOutcome[RdsAssessmentReport] = await(connector.acknowledgeRds(rdsAcknowledgementRequest, Some(rdsAuthCredentials)))

        result shouldBe Left(ErrorWrapper(correlationId, InternalError))
      }

      "return InternalError when UpstreamErrorResponse is returned" in new Test {
        mockRdsAcknowledgeException(UpstreamErrorResponse("test", BAD_GATEWAY, BAD_GATEWAY))

        val result: ServiceOutcome[RdsAssessmentReport] = await(connector.acknowledgeRds(rdsAcknowledgementRequest, Some(rdsAuthCredentials)))

        result shouldBe Left(ErrorWrapper(correlationId, InternalError))
      }

      "return InternalError when HttpException is returned" in new Test {
        mockRdsAcknowledgeException(new HttpException("test", SERVICE_UNAVAILABLE))

        val result: ServiceOutcome[RdsAssessmentReport] = await(connector.acknowledgeRds(rdsAcknowledgementRequest, Some(rdsAuthCredentials)))

        result shouldBe Left(ErrorWrapper(correlationId, InternalError))
      }

      "return InternalError when unknown Throwable is returned" in new Test {
        mockRdsAcknowledgeException(new RuntimeException("test"))

        val result: ServiceOutcome[RdsAssessmentReport] = await(connector.acknowledgeRds(rdsAcknowledgementRequest, Some(rdsAuthCredentials)))

        result shouldBe Left(ErrorWrapper(correlationId, InternalError))
      }
    }

    ".mapUpstreamError" should {
      "return ForbiddenDownstreamError for UNAUTHORIZED" in new Test {
        val result: ServiceOutcome[Nothing] = connector.mapUpstreamError(correlationId, UNAUTHORIZED)

        result shouldBe Left(ErrorWrapper(correlationId, ForbiddenDownstreamError))
      }

      "return ForbiddenDownstreamError for FORBIDDEN" in new Test {
        val result: ServiceOutcome[Nothing] = connector.mapUpstreamError(correlationId, FORBIDDEN)

        result shouldBe Left(ErrorWrapper(correlationId, ForbiddenDownstreamError))
      }

      "return InternalError for NOT_FOUND" in new Test {
        val result: ServiceOutcome[Nothing] = connector.mapUpstreamError(correlationId, NOT_FOUND)

        result shouldBe Left(ErrorWrapper(correlationId, InternalError))
      }
    }
  }

}
