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

package uk.gov.hmrc.selfassessmentassist.stubs

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status._
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.selfassessmentassist.support.WireMockMethods
import uk.gov.hmrc.selfassessmentassist.v1.TestData.CommonTestData

object AuthStub extends WireMockMethods {

  private val authoriseUri: String = "/auth/authorise"

  private val mtdEnrolment: JsObject = Json.obj(
    "key" -> "HMRC-MTD-IT",
    "identifiers" -> Json.arr(
      Json.obj(
        "key"   -> "MTDITID",
        "value" -> "1234567890"
      )
    )
  )

  def authorised(): StubMapping = {
    when(method = POST, uri = authoriseUri)
      .thenReturn(status = OK, body = successfulAuthResponse(mtdEnrolment))
  }

  def unauthorisedNotLoggedIn(): StubMapping = {
    when(method = POST, uri = authoriseUri)
      .thenReturn(status = UNAUTHORIZED, headers = Map("WWW-Authenticate" -> """MDTP detail="MissingBearerToken""""))
  }

  def unauthorisedOther(): StubMapping = {
    when(method = POST, uri = authoriseUri)
      .thenReturn(status = UNAUTHORIZED, headers = Map("WWW-Authenticate" -> """MDTP detail="InvalidBearerToken""""))
  }

  CommonTestData.acknowledgeSubmissionId
  private def successfulAuthResponse(enrolments: JsObject*): JsObject = {
    Json.obj(
      "authorisedEnrolments" -> enrolments,
      "affinityGroup"        -> "Individual",
      "allEnrolments"        -> enrolments,
      "internalId"           -> "12345",
      "externalId"           -> "6789",
      "agentCode"            -> "AGENT",
      "credentials" -> Json.obj(
        "providerId"   -> "id",
        "providerType" -> "PROVIDER"
      ),
      "confidenceLevel" -> 200,
      "nino"            -> "AA123456A",
      "saUtr"           -> "UTR",
      "name"            -> "Foo",
      "dateOfBirth"     -> "2011-12-03",
      "email"           -> "foo@foomail.com",
      "agentInformation" -> Json.obj(
        "agentId"   -> "id",
        "agentCode" -> "CODE"
      ),
      "groupIdentifier" -> "id",
      "credentialRole"  -> "Admin",
      "mdtpInformation" -> Json.obj(
        "deviceId" -> "ID",
          "sessionId" -> "sessionId"
      ),
      "credentialStrength" -> "strong",
      "loginTimes" -> Json.obj(
        "currentLogin"  -> "2023-04-01T09:00:00Z",
        "previousLogin" -> "2023-03-01T09:00:00Z"
      ),
      "itmpName" ->
          Json.obj(
            "givenName"  -> "foo",
            "middleName" -> "bar",
            "familyName" -> "baz"
          ),
      "itmpAddress" -> Json.obj(
        "line1" -> "23 Baz Bar Road",
        "postcode" -> "GG67YY"
      )
    )
  }

}
