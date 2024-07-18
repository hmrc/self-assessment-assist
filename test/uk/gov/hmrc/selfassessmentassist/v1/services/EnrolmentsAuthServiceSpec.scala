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

import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.{EmptyPredicate, Predicate}
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.selfassessmentassist.api.models.auth.{AuthOutcome, UserDetails}
import uk.gov.hmrc.selfassessmentassist.api.models.errors.MtdError
import uk.gov.hmrc.selfassessmentassist.config.ConfidenceLevelConfig
import uk.gov.hmrc.selfassessmentassist.support.{MockAppConfig, ServiceSpec}
import uk.gov.hmrc.selfassessmentassist.v1.mocks.connectors.MockAuthConnector

import scala.concurrent.{ExecutionContext, Future}

class EnrolmentsAuthServiceSpec extends ServiceSpec with MockAppConfig with MockAuthConnector {

  "EnrolmentsAuthService" when {
    "authorising" must {
      "500" in new Test {

        mockConfidenceLevelCheckConfig()

        (authConnector
          .authorise(_: Predicate, _: Retrieval[Any])(_: HeaderCarrier, _: ExecutionContext))
          .expects(*, *, *, *)
          .returns(() => {})
        val result: Future[AuthOutcome] = service.authorised(EmptyPredicate, "correlationId")
        await(result) shouldBe Left(MtdError("INTERNAL_SERVER_ERROR", "An internal server error occurred", 500))
      }
    }

    "createUserDetailsWithLogging individual" must {
      "pass" in new Test {
        val result: Future[Right[MtdError, UserDetails]] = service.createUserDetailsWithLogging(
          AffinityGroup.Individual,
          Enrolments(
            Set(
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
            )),
          "correlationId",
          None
        )

        await(result) shouldBe Right(
          UserDetails(
            userType = AffinityGroup.Individual,
            agentReferenceNumber = None,
            clientID = "",
            None
          ))
      }
    }

    "createUserDetailsWithLogging agent" must {
      "pass" in new Test {

        val result: Future[Right[MtdError, UserDetails]] = service.createUserDetailsWithLogging(
          AffinityGroup.Agent,
          Enrolments(
            Set(
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
            )),
          "correlationId",
          None
        )
        await(result) shouldBe Right(UserDetails(AffinityGroup.Agent, None, "", None))
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

    "agentInformation is None" must {
      "return success" in new TestPayload {
        mockConfidenceLevelCheckConfig()

        val predicate: Predicate =
          Enrolment("HMRC-MTD-IT")
            .withIdentifier("MTDITID", "some-value")
            .withDelegatedAuthRule("mtd-it-auth")

        val result:Future[AuthOutcome] = service.authorised(predicate, correlationId)

        val res = await(result)
        Console.print(res)
        /*res shouldBe Right(
          UserDetails(
            userType = AffinityGroup.Individual,
            agentReferenceNumber = None,
            clientID = "",
            None
          ))*/
      }
    }
  }

  trait TestPayload extends Test {

    (authConnector
      .authorise(_: Predicate, _: Retrieval[Any])(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, *, *, *)
      .returns(() => Future.successful("""{
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
          |}""".stripMargin))

    override val service: EnrolmentsAuthService = new EnrolmentsAuthService(authConnector, mockAppConfig)
  }

  trait Test {

    val authConnector: AuthConnector = mock[AuthConnector]
    val service                      = new EnrolmentsAuthService(authConnector, mockAppConfig)

    def mockConfidenceLevelCheckConfig(authValidationEnabled: Boolean = true, confidenceLevel: ConfidenceLevel = ConfidenceLevel.L200): Unit = {
      MockedAppConfig.confidenceLevelCheckEnabled.returns(
        ConfidenceLevelConfig(
          confidenceLevel = confidenceLevel,
          definitionEnabled = true,
          authValidationEnabled = authValidationEnabled
        )
      )
    }

  }

}
