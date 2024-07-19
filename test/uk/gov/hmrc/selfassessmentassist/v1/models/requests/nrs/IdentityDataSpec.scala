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

package uk.gov.hmrc.selfassessmentassist.v1.models.requests.nrs

import play.api.libs.json.Json
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.auth.core.{AffinityGroup, ConfidenceLevel, User}
import uk.gov.hmrc.selfassessmentassist.support.UnitSpec
import uk.gov.hmrc.selfassessmentassist.v1.models.request.nrs.IdentityData

import java.time.Instant

class IdentityDataSpec extends UnitSpec {

  val now = "2018-04-16T11:00:55Z"

  val payload = Json.parse(s"""{
      |  "confidenceLevel": 250,
      |  "credentials": {
      |    "providerId": "3485171374757954",
      |    "providerType": "GovernmentGateway"
      |  },
      |  "name": {
      |    "name": "TestUser"
      |  },
      |  "email": "user@test.com",
      |  "credentialStrength": "strong",
      |  "credentialRole": "User",
      |  "groupIdentifier": "testGroupId-f5c001fe-8007-469a-8526-c796e48f724d",
      |  "affinityGroup": "Individual",
      |  "agentInformation": {},
      |  "internalId": "Int-cb20d3af-a45c-46f4-a72d-a21a89fb3391",
      |  "externalId": "Ext-c69aceb4-0f5c-420e-89bf-d5f5d7f71372",
      |  "legacySaUserId": "ITajAusTTotWnoFaNIIM1Q@@",
      |  "nino": "PW872433A",
      |  "loginTimes": {"currentLogin": "$now"},
      |  "itmpName": {},
      |  "itmpAddress": {},
      |  "allEnrolments": [
      |    {
      |      "key": "HMRC-MTD-IT",
      |      "identifiers": [
      |        {
      |          "key": "MTDITID",
      |          "value": "XZIT00000564795"
      |                 }
      |      ],
      |      "state": "Activated",
      |      "confidenceLevel": 50
      |    },
      |    {
      |      "key": "HMRC-NI",
      |      "identifiers": [
      |        {
      |          "key": "NINO",
      |          "value": "PW872433A"
      |                 }
      |      ],
      |      "state": "Activated",
      |      "confidenceLevel": 250
      |      }
      |  ]
      |}""".stripMargin)

  val model: IdentityData = IdentityData(
    internalId = Some("Int-cb20d3af-a45c-46f4-a72d-a21a89fb3391"),
    externalId = Some("Ext-c69aceb4-0f5c-420e-89bf-d5f5d7f71372"),
    agentCode = None,
    credentials = Some(Credentials(providerId = "3485171374757954", providerType = "GovernmentGateway")),
    confidenceLevel = ConfidenceLevel.L250,
    nino = Some("PW872433A"),
    saUtr = None,
    name = Some(Name(Some("TestUser"), None)),
    dateOfBirth = None,
    email = Some("user@test.com"),
    agentInformation = AgentInformation(None, None, None),
    groupIdentifier = Some("testGroupId-f5c001fe-8007-469a-8526-c796e48f724d"),
    credentialRole = Some(User),
    mdtpInformation = None,
    itmpName = ItmpName(None, None, None),
    itmpDateOfBirth = None,
    itmpAddress = ItmpAddress(None, None, None, None, None, None, None, None),
    affinityGroup = Some(AffinityGroup.Individual),
    credentialStrength = Some("strong"),
    loginTimes = Some(LoginTimes(currentLogin = Instant.parse(now), previousLogin = None))
  )

  "Payload" should {
    "correctly map JSON to IdentityData" in {
      payload.as[IdentityData] shouldBe model
    }
  }

}
