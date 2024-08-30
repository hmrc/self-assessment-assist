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

import play.api.libs.json.JsValue
import play.api.libs.ws.{WSRequest, WSResponse}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.selfassessmentassist.stubs.DownstreamStub

class SelfAssessmentAssistAuthSupportingAgentsAllowedISpec extends AuthSupportingAgentsAllowedISpec {

  val callingApiVersion = "1.0"

  val supportingAgentsAllowedEndpoint = "list-all-businesses"

  val mtdUrl = s"/$nino/list"

  def sendMtdRequest(request: WSRequest): WSResponse = await(request.get())

  val downstreamUri = s"/registration/business-details/nino/$nino"

  override val downstreamHttpMethod: DownstreamStub.HTTPMethod = DownstreamStub.GET

  val maybeDownstreamResponseJson: Option[JsValue] = None

}
