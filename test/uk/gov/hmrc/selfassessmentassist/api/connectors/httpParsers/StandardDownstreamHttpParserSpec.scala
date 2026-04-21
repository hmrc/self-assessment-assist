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

package uk.gov.hmrc.selfassessmentassist.api.connectors.httpParsers

import play.api.http.Status.*
import play.api.libs.json.{JsValue, Json, Reads}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import uk.gov.hmrc.selfassessmentassist.api.connectors.DownstreamOutcome
import uk.gov.hmrc.selfassessmentassist.api.models.errors.{DownstreamErrorCode, DownstreamErrors, InternalError, OutboundError}
import uk.gov.hmrc.selfassessmentassist.api.models.outcomes.ResponseWrapper
import uk.gov.hmrc.selfassessmentassist.support.UnitSpec

class StandardDownstreamHttpParserSpec extends UnitSpec {

  // WLOG if Reads tested elsewhere
  case class SomeDataObject(data: String)

  object SomeDataObject {
    implicit val reads: Reads[SomeDataObject] = Json.reads
  }

  val method = "POST"
  val url    = "test-url"

  val correlationId = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"

  import uk.gov.hmrc.selfassessmentassist.api.connectors.httpParsers.StandardDownstreamHttpParser.*

  val httpReads: HttpReads[DownstreamOutcome[Unit]] = implicitly

  val data                  = "someData"
  val expectedJson: JsValue = Json.obj("data" -> data)

  val downstreamResponseData: SomeDataObject              = SomeDataObject(data)
  val downstreamResponse: ResponseWrapper[SomeDataObject] = ResponseWrapper(correlationId, downstreamResponseData)

  "The generic HTTP parser" when {
    "no status code is specified" should {
      val httpReads: HttpReads[DownstreamOutcome[SomeDataObject]] = implicitly

      "return a Right downstream response containing the data object" in {
        val httpResponse = HttpResponse(OK, expectedJson.toString(), Map("CorrelationId" -> Seq(correlationId)))

        httpReads.read(method, url, httpResponse) shouldBe Right(downstreamResponse)
      }

      "return an outbound error if a data object cannot be read from the response json" in {
        val badFieldTypeJson: JsValue = Json.obj("incomeSourceId" -> 1234, "incomeSourceName" -> 1234)
        val httpResponse              = HttpResponse(OK, badFieldTypeJson.toString(), Map("CorrelationId" -> Seq(correlationId)))
        val expected                  = ResponseWrapper(correlationId, OutboundError(InternalError))

        httpReads.read(method, url, httpResponse) shouldBe Left(expected)
      }

      handleErrorsCorrectly(httpReads)
      handleInternalErrorsCorrectly(httpReads)
      handleUnexpectedResponse(httpReads)
    }

    "a success code is specified" should {
      "use that status code for success" in {
        implicit val successCode: SuccessCode                       = SuccessCode(PARTIAL_CONTENT)
        val httpReads: HttpReads[DownstreamOutcome[SomeDataObject]] = implicitly

        val httpResponse = HttpResponse(PARTIAL_CONTENT, expectedJson.toString(), Map("CorrelationId" -> Seq(correlationId)))

        httpReads.read(method, url, httpResponse) shouldBe Right(downstreamResponse)
      }
    }
  }

  "The generic HTTP parser for empty response" when {
    "no status code is specified" should {
      val httpReads: HttpReads[DownstreamOutcome[Unit]] = implicitly

      "receiving a 204 response" should {
        "return a Right downstream Response with the correct correlationId and no responseData" in {
          val httpResponse = HttpResponse(NO_CONTENT, "", headers = Map("CorrelationId" -> Seq(correlationId)))

          httpReads.read(method, url, httpResponse) shouldBe Right(ResponseWrapper(correlationId, ()))
        }
      }

      handleErrorsCorrectly(httpReads)
      handleInternalErrorsCorrectly(httpReads)
      handleUnexpectedResponse(httpReads)
    }

    "a success code is specified" should {
      implicit val successCode: SuccessCode             = SuccessCode(PARTIAL_CONTENT)
      val httpReads: HttpReads[DownstreamOutcome[Unit]] = implicitly

      "use that status code for success" in {
        val httpResponse = HttpResponse(PARTIAL_CONTENT, "", headers = Map("CorrelationId" -> Seq(correlationId)))

        httpReads.read(method, url, httpResponse) shouldBe Right(ResponseWrapper(correlationId, ()))
      }
    }
  }

  val singleErrorJson: JsValue = Json.parse(
    """
      |{
      |   "code": "CODE",
      |   "reason": "ignored"
      |}
    """.stripMargin
  )

  val multipleErrorsJson: JsValue = Json.parse(
    """
      |{
      |   "failures": [
      |       {
      |           "code": "CODE 1",
      |           "reason": "ignored"
      |       },
      |       {
      |           "code": "CODE 2",
      |           "reason": "ignored"
      |       }
      |   ]
      |}
    """.stripMargin
  )

  val malformedErrorJson: JsValue = Json.parse(
    """
      |{
      |   "coed": "CODE",
      |   "resaon": "MESSAGE"
      |}
    """.stripMargin
  )

  private def handleErrorsCorrectly[A](httpReads: HttpReads[DownstreamOutcome[A]]): Unit =
    List(BAD_REQUEST, NOT_FOUND, FORBIDDEN, CONFLICT, GONE, UNPROCESSABLE_ENTITY).foreach(responseCode =>
      s"receiving a $responseCode response" should {
        "be able to parse a single error" in {
          val httpResponse = HttpResponse(responseCode, singleErrorJson.toString(), Map("CorrelationId" -> Seq(correlationId)))

          httpReads.read(method, url, httpResponse) shouldBe Left(
            ResponseWrapper(correlationId, DownstreamErrors.single(DownstreamErrorCode("CODE"))))
        }

        "be able to parse multiple errors" in {
          val httpResponse = HttpResponse(responseCode, multipleErrorsJson.toString(), Map("CorrelationId" -> Seq(correlationId)))

          httpReads.read(method, url, httpResponse) shouldBe {
            Left(ResponseWrapper(correlationId, DownstreamErrors(List(DownstreamErrorCode("CODE 1"), DownstreamErrorCode("CODE 2")))))
          }
        }

        "return an outbound error when the error returned doesn't match the Error model" in {
          val httpResponse = HttpResponse(responseCode, malformedErrorJson.toString(), Map("CorrelationId" -> Seq(correlationId)))

          httpReads.read(method, url, httpResponse) shouldBe Left(ResponseWrapper(correlationId, OutboundError(InternalError)))
        }
      })

  private def handleInternalErrorsCorrectly[A](httpReads: HttpReads[DownstreamOutcome[A]]): Unit =
    List(INTERNAL_SERVER_ERROR, SERVICE_UNAVAILABLE).foreach(responseCode =>
      s"receiving a $responseCode response" should {
        "return an outbound error" in {
          val httpResponse = HttpResponse(responseCode, singleErrorJson.toString(), Map("CorrelationId" -> Seq(correlationId)))

          httpReads.read(method, url, httpResponse) shouldBe Left(ResponseWrapper(correlationId, OutboundError(InternalError)))
        }
      })

  private def handleUnexpectedResponse[A](httpReads: HttpReads[DownstreamOutcome[A]]): Unit =
    "receiving an unexpected response" should {
      val responseCode = 499

      "return an outbound error" in {
        val httpResponse = HttpResponse(responseCode, singleErrorJson.toString(), Map("CorrelationId" -> Seq(correlationId)))

        httpReads.read(method, url, httpResponse) shouldBe Left(ResponseWrapper(correlationId, OutboundError(InternalError)))
      }
    }

}
