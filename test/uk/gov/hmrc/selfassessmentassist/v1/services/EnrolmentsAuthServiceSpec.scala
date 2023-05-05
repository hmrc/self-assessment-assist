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

package uk.gov.hmrc.selfassessmentassist.v1.services

import uk.gov.hmrc.auth.core.AffinityGroup.Individual
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.{EmptyPredicate, Predicate}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{affinityGroup, allEnrolments}
import uk.gov.hmrc.auth.core.retrieve.{~, Retrieval}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.selfassessmentassist.support.{MockAppConfig, ServiceSpec}
import uk.gov.hmrc.selfassessmentassist.v1.mocks.connectors.MockAuthConnector
import uk.gov.hmrc.selfassessmentassist.v1.models.auth.{AuthOutcome, UserDetails}
import uk.gov.hmrc.selfassessmentassist.v1.models.errors.MtdError
import uk.gov.hmrc.selfassessmentassist.v1.services.AuthFixture.enrolments

import scala.concurrent.{ExecutionContext, Future}

class EnrolmentsAuthServiceSpec extends ServiceSpec with MockAppConfig with MockAuthConnector {

  class Test {
    val authConnector = mock[AuthConnector]
    val service = new EnrolmentsAuthService(authConnector)
  }

  "EnrolmentsAuthService" when {
    "authorising" must {
      "500" in new Test {

        (authConnector.authorise( _: Predicate, _: Retrieval[Any])( _:HeaderCarrier, _: ExecutionContext))
          .expects(*, *,*, *).returns(() => {})
        val result: Future[AuthOutcome] = service.authorised(EmptyPredicate, "correlationId")
        await(result) shouldBe Left(MtdError("INTERNAL_SERVER_ERROR","An internal server error occurred",None))
      }
    }

    "createUserDetailsWithLogging individual" must {
      "pass" in new Test {
        val result: Future[Right[MtdError, UserDetails]] = service.createUserDetailsWithLogging(AffinityGroup.Individual, Enrolments(Set(
          Enrolment(
            key = "MDTP-IT",
            identifiers = Seq(
              EnrolmentIdentifier(
                "UTR",
                "123"
              )
            ),
            state = "Activated"
          )
        )), "correlationId", None)

        await(result) shouldBe Right(UserDetails(
          userType = AffinityGroup.Individual,
          agentReferenceNumber = None,
          clientID = "",
          None
        ))
      }
    }

    "createUserDetailsWithLogging agent" must {
      "pass" in new Test {

        val result: Future[Right[MtdError, UserDetails]] = service.createUserDetailsWithLogging(AffinityGroup.Agent, Enrolments(Set(
          Enrolment(
            key = "MDTP-IT",
            identifiers = Seq(
              EnrolmentIdentifier(
                "UTR",
                "123"
              )
            ),
            state = "Activated"
          )
        )), "correlationId", None)
        await(result) shouldBe Right(UserDetails(AffinityGroup.Agent,None,"",None))
      }
    }

    "getClientReferenceFromEnrolments" when {
      "a valid enrolment with a MTDITID exists" should {
        "return the expected result" in new Test {

          val enrolments: Enrolments =
            Enrolments(
              enrolments = Set(
                Enrolment(
                  key = "HMRC-MTD-IT",
                  identifiers = Seq(
                    EnrolmentIdentifier(
                      "MTDITID",
                      "123"
                    )
                  ),
                  state = "Activated"
                )
              )
            )

          service.getClientReferenceFromEnrolments(enrolments) shouldBe Some("123")
        }
      }

      "a valid enrolment with a UTR does not exist" should {
        "return None" in new Test {

          val enrolments: Enrolments =
            Enrolments(
              enrolments = Set(
                Enrolment(
                  key = "MDTP-IT",
                  identifiers = Seq(
                    EnrolmentIdentifier(
                      "UTR",
                      "123"
                    )
                  ),
                  state = "Activated"
                )
              )
            )

          service.getClientReferenceFromEnrolments(enrolments) shouldBe None
        }
      }
    }

  }
}
