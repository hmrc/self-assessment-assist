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

package uk.gov.hmrc.selfassessmentassist.api.auth

import play.api.http.Status.NO_CONTENT
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{WSRequest, WSResponse}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.selfassessmentassist.api.models.domain.TaxYear
import uk.gov.hmrc.selfassessmentassist.stubs.{CommonTestData, DownstreamStub}

class SelfAssessmentAssistAuthMainAgentsOnlyISpec extends AuthMainAgentsOnlyISpec {

  val callingApiVersion = "1.0"

  val supportingAgentsNotAllowedEndpoint = "generate-report"

  private val businessId = "XAIS12345678901"
  private val taxYear    = TaxYear.fromMtd("2018-19")

  def calculationId: String = CommonTestData.simpleCalculationId.toString

  val mtdUrl = s"/reports/$nino/$taxYear/$calculationId"

  def sendMtdRequest(request: WSRequest): WSResponse = await(request.put(Json.parse("""{}""")))

  val downstreamUri: String =
    s"/income-tax/${taxYear}/income-sources/reporting-type/$nino/$businessId"

  val maybeDownstreamResponseJson: Option[JsValue] = Some(
    Json.parse("""
                 |{
                 | "QRT": "Standard"
                 |}
                 |""".stripMargin)
  )

  override val downstreamHttpMethod: DownstreamStub.HTTPMethod = DownstreamStub.PUT

  override val expectedMtdSuccessStatus: Int = NO_CONTENT

}

