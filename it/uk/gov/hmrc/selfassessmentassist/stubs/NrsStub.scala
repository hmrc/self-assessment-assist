/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.selfassessmentassist.stubs

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.Json

object NrsStub {

  def submit(url: String): StubMapping = {
    DownstreamStub.onSuccess(DownstreamStub.POST, url, OK, Json.toJson(CommonTestData.simpleNRSResponseAcknowledgeSubmission))
  }

  def submitFailure(url: String): StubMapping = {
    DownstreamStub.onError(DownstreamStub.POST, url, INTERNAL_SERVER_ERROR, "Internal Server Error")
  }

}
