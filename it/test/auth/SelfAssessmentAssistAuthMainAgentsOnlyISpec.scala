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

package auth

import play.api.http.Status.OK
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{WSRequest, WSResponse}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.selfassessmentassist.stubs._

class SelfAssessmentAssistAuthMainAgentsOnlyISpec extends AuthMainAgentsOnlyISpec {

  val callingApiVersion = "1.0"

  val supportingAgentsNotAllowedEndpoint = "generate-report"

  val taxYear: String = "2018-19"

  def calculationId: String = CommonTestData.simpleCalculationId.toString

  val mtdUrl = s"/reports/$nino/$taxYear/$calculationId"

  val emptyJson: JsValue = Json.parse("""{}""")

  override protected def sendMtdRequest(request: WSRequest): WSResponse = await(request.post(emptyJson))

  override val expectedMtdSuccessStatus: Int = OK

}
