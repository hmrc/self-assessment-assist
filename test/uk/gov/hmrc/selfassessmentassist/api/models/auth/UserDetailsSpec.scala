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

package uk.gov.hmrc.selfassessmentassist.api.models.auth

import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.selfassessmentassist.api.models.domain.CustomerType
import uk.gov.hmrc.selfassessmentassist.api.models.domain.CustomerType.CustomerType
import uk.gov.hmrc.selfassessmentassist.support.UnitSpec

class UserDetailsSpec extends UnitSpec {

  "toCustomerType" should {

    "return TaxPayer for Individual" in {
      val details = UserDetails(
        userType = AffinityGroup.Individual,
        agentReferenceNumber = None,
        clientID = "client-123"
      )

      details.toCustomerType shouldBe CustomerType.TaxPayer
    }

    "return TaxPayer for Organisation" in {
      val details = UserDetails(
        userType = AffinityGroup.Organisation,
        agentReferenceNumber = None,
        clientID = "client-123"
      )

      details.toCustomerType shouldBe CustomerType.TaxPayer
    }

    "return Agent for Agent" in {
      val details = UserDetails(
        userType = AffinityGroup.Agent,
        agentReferenceNumber = Some("ARN123"),
        clientID = "client-123"
      )

      details.toCustomerType shouldBe CustomerType.Agent
    }

    "throw an exception for an unsupported affinity group" in {
      val invalidGroup = mock[AffinityGroup]

      an[IllegalStateException] shouldBe thrownBy {
        UserDetails(
          userType = invalidGroup,
          agentReferenceNumber = None,
          clientID = "client-123"
        )
      }
    }
  }

}
