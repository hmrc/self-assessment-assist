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

package uk.gov.hmrc.selfassessmentassist.v1.models.requests.nrs

import uk.gov.hmrc.selfassessmentassist.support.UnitSpec
import play.api.libs.json.Json
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.retrieve.*
import java.time.{Instant, LocalDate}
import uk.gov.hmrc.selfassessmentassist.v1.models.request.nrs.IdentityData

class IdentityDataSpec extends UnitSpec {

  "IdentityData JSON format" should {

    "round-trip successfully with populated fields" in {

      val identityData = IdentityData(
        internalId = Some("internal-id"),
        externalId = Some("external-id"),
        agentCode = Some("agent-code"),
        credentials = Some(
          Credentials(
            providerId = "provider",
            providerType = "id"
          )
        ),
        confidenceLevel = ConfidenceLevel.L200,
        nino = Some("AA123456A"),
        saUtr = Some("1234567890"),
        dateOfBirth = Some(LocalDate.parse("1990-01-01")),
        email = Some("test@example.com"),
        agentInformation = AgentInformation(
          agentCode = Some("AGENT123"),
          agentFriendlyName = Some("Test Agent"),
          agentId = Some("agent-id-123")
        ),
        groupIdentifier = Some("group-id"),
        credentialRole = Some(User),
        mdtpInformation = Some(
          MdtpInformation(
            deviceId = "device-id",
            sessionId = "session-id"
          )
        ),
        itmpName = ItmpName(
          givenName = Some("Jane"),
          familyName = Some("Doe"),
          middleName = None
        ),
        itmpDateOfBirth = Some(LocalDate.parse("1990-01-01")),
        itmpAddress = ItmpAddress(
          line1 = Some("1 Test Street"),
          line2 = None,
          line3 = None,
          line4 = None,
          line5 = None,
          postCode = Some("AA1 1AA"),
          countryName = Some("GB"),
          countryCode = None
        ),
        affinityGroup = Some(AffinityGroup.Individual),
        credentialStrength = Some("strong"),
        loginTimes = LoginTimes(
          currentLogin = Instant.parse("2024-01-01T10:00:00Z"),
          previousLogin = Some(Instant.parse("2023-12-31T10:00:00Z"))
        )
      )

      Json.toJson(identityData).as[IdentityData] shouldBe identityData
    }
  }

}
