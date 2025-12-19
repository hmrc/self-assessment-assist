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
import play.api.http.Status.*
import play.api.libs.json.{JsArray, JsObject, JsValue, Json}
import uk.gov.hmrc.auth.core.Enrolment
import uk.gov.hmrc.selfassessmentassist.support.WireMockMethods
import uk.gov.hmrc.selfassessmentassist.v1.services.EnrolmentsAuthService

object AuthStub extends WireMockMethods {

  private val authoriseUri: String = "/auth/authorise"

  def authorised(): StubMapping = authorisedWithIndividualAffinityGroup()

  def unauthorisedNotLoggedIn(): StubMapping = {
    when(method = POST, uri = authoriseUri)
      .thenReturn(status = UNAUTHORIZED, headers = Map("WWW-Authenticate" -> """MDTP detail="MissingBearerToken""""))
  }

  def unauthorisedOther(message: String): StubMapping = {
    when(method = POST, uri = authoriseUri)
      .thenReturn(status = UNAUTHORIZED, headers = Map("WWW-Authenticate" -> s"""MDTP detail="$message""""))
  }

  private def successfulAuthResponse(affinityGroup: String, enrolments: Seq[JsObject]): JsObject = {
    Json.obj(
      "authorisedEnrolments" -> enrolments,
      "affinityGroup"        -> affinityGroup,
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
        "deviceId"  -> "ID",
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
        "line1"    -> "23 Baz Bar Road",
        "postcode" -> "GG67YY"
      )
    )
  }

  def authorisedWithIndividualAffinityGroup(): StubMapping =
    authorisedWith(affinityGroup = "Individual")

  def authorisedWithAgentAffinityGroup(): StubMapping =
    authorisedWith(affinityGroup = "Agent")

  def authorisedWithPrimaryAgentEnrolment(): StubMapping =
    when(method = POST, uri = authoriseUri)
      .withRequestBody(
        followUpAgentEnrolmentsRequest(EnrolmentsAuthService.mtdEnrolmentPredicate(mtdId = "1234567890"))
      )
      .thenReturn(status = OK, body = JsObject.empty)

  def unauthorisedForPrimaryAgentEnrolment(): StubMapping =
    when(method = POST, uri = authoriseUri)
      .withRequestBody(
        followUpAgentEnrolmentsRequest(EnrolmentsAuthService.mtdEnrolmentPredicate(mtdId = "1234567890"))
      )
      .thenReturn(status = UNAUTHORIZED, headers = Map("WWW-Authenticate" -> """MDTP detail="FailedRelationship""""))

  def authorisedWithSupportingAgentEnrolment(): StubMapping =
    when(method = POST, uri = authoriseUri)
      .withRequestBody(
        followUpAgentEnrolmentsRequest(EnrolmentsAuthService.supportingAgentAuthPredicate(mtdId = "1234567890"))
      )
      .thenReturn(status = OK, body = JsObject.empty)

  def unauthorisedForSupportingAgentEnrolment(): StubMapping =
    when(method = POST, uri = authoriseUri)
      .withRequestBody(
        followUpAgentEnrolmentsRequest(EnrolmentsAuthService.supportingAgentAuthPredicate(mtdId = "1234567890"))
      )
      .thenReturn(status = UNAUTHORIZED, headers = Map("WWW-Authenticate" -> """MDTP detail="FailedRelationship""""))

  def unauthorisedOther(): StubMapping =
    when(method = POST, uri = authoriseUri)
      .thenReturn(status = UNAUTHORIZED, headers = Map("WWW-Authenticate" -> """MDTP detail="InvalidBearerToken""""))

  private def authorisedWith(affinityGroup: String): StubMapping =
    when(method = POST, uri = authoriseUri)
      .withRequestBody(initialRetrievalsRequestBody)
      .thenReturn(
        status = OK,
        body = successfulAuthResponse(
          affinityGroup = affinityGroup,
          enrolments = initialAgentEnrolmentResponse
        ))

  private def followUpAgentEnrolmentsRequest(enrolment: Enrolment): JsValue = {
    JsObject(
      Map(
        "authorise" -> JsArray(List(enrolment.toJson)),
        "retrieve"  -> JsArray.empty
      ))
  }

  private val initialAgentEnrolmentResponse = List(
    Json.obj(
      "key" -> "HMRC-AS-AGENT",
      "identifiers" -> Json.arr(
        Json.obj(
          "key"   -> "AgentReferenceNumber",
          "value" -> "1234567890"
        )
      )
    )
  )

  private val initialRetrievalsRequestBody =
    Json.parse("""
      |{
      |	"authorise": [
      |		{
      |		    "$or":
      |		    [
      |		        [
      |		            {
      |		                "affinityGroup": "Individual"
      |		            },
      |		            {
      |		                "confidenceLevel": 200
      |		            },
      |		            {
      |		                "identifiers":
      |		                [
      |		                    {
      |		                        "key": "MTDITID",
      |		                        "value": "1234567890"
      |		                    }
      |		                ],
      |		                "state": "Activated",
      |		                "delegatedAuthRule": "mtd-it-auth",
      |		                "enrolment": "HMRC-MTD-IT"
      |		            }
      |		        ],
      |		        [
      |		            {
      |		                "affinityGroup": "Organisation"
      |		            },
      |		            {
      |		                "identifiers":
      |		                [
      |		                    {
      |		                        "key": "MTDITID",
      |		                        "value": "1234567890"
      |		                    }
      |		                ],
      |		                "state": "Activated",
      |		                "delegatedAuthRule": "mtd-it-auth",
      |		                "enrolment": "HMRC-MTD-IT"
      |		            }
      |		        ],
      |		        [
      |		            {
      |		                "affinityGroup": "Agent"
      |		            },
      |		            {
      |		                "identifiers":
      |		                [],
      |		                "state": "Activated",
      |		                "enrolment": "HMRC-AS-AGENT"
      |		            }
      |		        ]
      |		    ]
      |		}
      |	],
      |	"retrieve": [
      |     "affinityGroup",
      |     "allEnrolments",
      |     "internalId",
      |     "externalId",
      |     "agentCode",
      |     "optionalCredentials",
      |     "confidenceLevel",
      |     "nino",
      |     "saUtr",
      |     "dateOfBirth",
      |     "email",
      |     "agentInformation",
      |     "groupIdentifier",
      |     "credentialRole",
      |     "mdtpInformation",
      |     "credentialStrength",
      |     "loginTimes",
      |     "optionalItmpName",
      |     "optionalItmpAddress"
      |	]
      |}
      |""".stripMargin)

}
