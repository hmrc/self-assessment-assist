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

import org.scalamock.handlers.CallHandler
import play.api.libs.json.Json
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual, Organisation}
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.*
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.*
import uk.gov.hmrc.auth.core.syntax.retrieved.*
import uk.gov.hmrc.auth.core.{
  AuthConnector,
  ConfidenceLevel,
  Enrolment,
  EnrolmentIdentifier,
  Enrolments,
  InsufficientEnrolments,
  MissingBearerToken,
  *
}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.selfassessmentassist.api.models.auth.{AuthOutcome, UserDetails}
import uk.gov.hmrc.selfassessmentassist.api.models.errors.{ClientOrAgentNotAuthorisedError, InternalError}
import uk.gov.hmrc.selfassessmentassist.config.ConfidenceLevelConfig
import uk.gov.hmrc.selfassessmentassist.support.{MockAppConfig, ServiceSpec}
import uk.gov.hmrc.selfassessmentassist.v1.models.request.nrs.IdentityData
import uk.gov.hmrc.selfassessmentassist.v1.services.EnrolmentsAuthService.{
  authorisationDisabledPredicate,
  authorisationEnabledPredicate,
  mtdEnrolmentPredicate,
  supportingAgentAuthPredicate
}

import scala.concurrent.{ExecutionContext, Future}

class EnrolmentsAuthServiceSpec extends ServiceSpec with MockAppConfig {

  val mtdId = "123567890"

  "calling .authorised" when {

    "confidence level checks are on" should {
      behave like authService(
        authValidationEnabled = true,
        authorisationEnabledPredicate(mtdId),
        mtdEnrolmentPredicate(mtdId),
        supportingAgentAuthPredicate(mtdId)
      )
    }

    "confidence level checks are off" should {
      behave like authService(
        authValidationEnabled = false,
        authorisationDisabledPredicate(mtdId),
        mtdEnrolmentPredicate(mtdId),
        supportingAgentAuthPredicate(mtdId)
      )
    }

    def authService(
        authValidationEnabled: Boolean,
        initialPredicate: Predicate,
        primaryAgentPredicate: Predicate,
        supportingAgentPredicate: Predicate
    ): Unit = {
      behave like authorisedIndividual(authValidationEnabled, initialPredicate)
      behave like authorisedOrganisation(authValidationEnabled, initialPredicate)

      behave like authorisedAgentsMissingArn(authValidationEnabled, initialPredicate)
      behave like authorisedPrimaryAgent(authValidationEnabled, initialPredicate, primaryAgentPredicate)
      behave like authorisedSupportingAgent(authValidationEnabled, initialPredicate, primaryAgentPredicate, supportingAgentPredicate)

      behave like disallowSupportingAgentForPrimaryOnlyEndpoint(authValidationEnabled, initialPredicate, primaryAgentPredicate)

      behave like disallowUsersWithoutEnrolments(authValidationEnabled, initialPredicate)
      behave like disallowWhenNoBearerToken(authValidationEnabled, initialPredicate)
    }

    def authorisedIndividual(authValidationEnabled: Boolean, initialPredicate: Predicate): Unit =
      "allow authorised individuals" in new Test {
        mockConfidenceLevelCheckConfig(authValidationEnabled = authValidationEnabled)

        val retrievalsResult = getRetrievalsResult(Some(Individual), Enrolments(Set.empty))

        MockedAuthConnector
          .authorised(initialPredicate, retrievals)
          .once()
          .returns(Future.successful(retrievalsResult))

        val result: AuthOutcome = await(enrolmentsAuthService.authorised(mtdId, "correlationId", endpointAllowsSupportingAgents = true))
        result shouldBe Right(
          UserDetails(
            userType = AffinityGroup.Individual,
            agentReferenceNumber = None,
            clientID = "",
            Some(getIdentityData(agentInformation = AgentInformation(None, None, None), affinityGroup = Some(Individual)))
          )
        )
      }

    def authorisedOrganisation(authValidationEnabled: Boolean, initialPredicate: Predicate): Unit =
      "allow authorised organisations" in new Test {
        mockConfidenceLevelCheckConfig(authValidationEnabled = authValidationEnabled)

        val retrievalsResult = getRetrievalsResult(Some(Organisation), Enrolments(Set.empty))

        MockedAuthConnector
          .authorised(initialPredicate, retrievals)
          .once()
          .returns(Future.successful(retrievalsResult))

        val result: AuthOutcome = await(enrolmentsAuthService.authorised(mtdId, "correlationId", endpointAllowsSupportingAgents = true))
        result shouldBe Right(
          UserDetails(
            userType = AffinityGroup.Organisation,
            agentReferenceNumber = None,
            clientID = "",
            Some(getIdentityData(agentInformation = AgentInformation(None, None, None), affinityGroup = Some(Organisation)))
          )
        )
      }

    def authorisedAgentsMissingArn(
        authValidationEnabled: Boolean,
        initialPredicate: Predicate
    ): Unit =
      "disallow agents that are missing an ARN" in new Test {

        val enrolments: Enrolments = Enrolments(
          Set(Enrolment("HMRC-AS-AGENT", List(EnrolmentIdentifier("SomeOtherIdentifier", "123567890")), "Active"))
        )
        val initialRetrievalsResult = getRetrievalsResult(Some(Agent), enrolments)

        MockedAuthConnector
          .authorised(initialPredicate, retrievals)
          .once()
          .returns(Future.successful(initialRetrievalsResult))

        mockConfidenceLevelCheckConfig(authValidationEnabled = authValidationEnabled)

        val result: AuthOutcome = await(enrolmentsAuthService.authorised(mtdId, "correlationId", endpointAllowsSupportingAgents = true))
        result shouldBe Left(InternalError)
      }

    def authorisedPrimaryAgent(
        authValidationEnabled: Boolean,
        initialPredicate: Predicate,
        primaryAgentPredicate: Predicate
    ): Unit =
      "allow authorised Primary agents with ARN" in new Test {
        val arn = "123567890"
        val enrolments: Enrolments = Enrolments(
          Set(
            Enrolment(
              "HMRC-AS-AGENT",
              List(EnrolmentIdentifier("AgentReferenceNumber", arn)),
              "Active"
            ))
        )

        val initialRetrievalsResult = getRetrievalsResult(Some(Agent), enrolments)

        MockedAuthConnector
          .authorised(initialPredicate, retrievals)
          .once()
          .returns(Future.successful(initialRetrievalsResult))

        MockedAuthConnector
          .authorised(primaryAgentPredicate, EmptyRetrieval)
          .once()
          .returns(Future.successful(EmptyRetrieval))

        mockConfidenceLevelCheckConfig(authValidationEnabled = authValidationEnabled)

        val result: AuthOutcome = await(enrolmentsAuthService.authorised("123567890", "correlationId", endpointAllowsSupportingAgents = true))
        result shouldBe Right(
          UserDetails(
            userType = AffinityGroup.Agent,
            agentReferenceNumber = Some(arn),
            clientID = "",
            Some(getIdentityData(agentInformation = AgentInformation(None, None, None), affinityGroup = Some(Agent)))
          )
        )
      }

    def authorisedSupportingAgent(
        authValidationEnabled: Boolean,
        initialPredicate: Predicate,
        primaryAgentPredicate: Predicate,
        supportingAgentPredicate: Predicate
    ): Unit =
      "allow authorised Supporting agents with ARN" in new Test {
        val arn = "123567890"
        val enrolments: Enrolments = Enrolments(
          Set(
            Enrolment(
              "HMRC-AS-AGENT",
              List(EnrolmentIdentifier("AgentReferenceNumber", arn)),
              "Active"
            ))
        )

        val initialRetrievalsResult = getRetrievalsResult(Some(Agent), enrolments)

        MockedAuthConnector
          .authorised(initialPredicate, retrievals)
          .once()
          .returns(Future.successful(initialRetrievalsResult))

        MockedAuthConnector
          .authorised(primaryAgentPredicate, EmptyRetrieval)
          .once()
          .returns(Future.failed(InsufficientEnrolments()))

        MockedAuthConnector
          .authorised(supportingAgentPredicate, EmptyRetrieval)
          .once()
          .returns(Future.successful(EmptyRetrieval))

        mockConfidenceLevelCheckConfig(authValidationEnabled = authValidationEnabled)

        val result: AuthOutcome = await(enrolmentsAuthService.authorised(mtdId, "correlationId", endpointAllowsSupportingAgents = true))
        result shouldBe Right(
          UserDetails(
            userType = AffinityGroup.Agent,
            agentReferenceNumber = Some(arn),
            clientID = "",
            Some(getIdentityData(agentInformation = AgentInformation(None, None, None), affinityGroup = Some(Agent)))
          )
        )
      }

    def disallowSupportingAgentForPrimaryOnlyEndpoint(
        authValidationEnabled: Boolean,
        initialPredicate: Predicate,
        primaryAgentPredicate: Predicate
    ): Unit =
      "disallow Supporting agents for a primary-only endpoint" in new Test {
        val arn = "123567890"
        val enrolments: Enrolments = Enrolments(
          Set(
            Enrolment(
              "HMRC-AS-AGENT",
              List(EnrolmentIdentifier("AgentReferenceNumber", arn)),
              "Active"
            ))
        )

        val initialRetrievalsResult = getRetrievalsResult(Some(Agent), enrolments)

        MockedAuthConnector
          .authorised(initialPredicate, retrievals)
          .once()
          .returns(Future.successful(initialRetrievalsResult))

        MockedAuthConnector
          .authorised(primaryAgentPredicate, EmptyRetrieval)
          .once()
          .returns(Future.failed(InsufficientEnrolments()))

        mockConfidenceLevelCheckConfig(authValidationEnabled = authValidationEnabled)

        val result: AuthOutcome = await(enrolmentsAuthService.authorised(mtdId, "correlationId", endpointAllowsSupportingAgents = false))
        result shouldBe Left(ClientOrAgentNotAuthorisedError)
      }

    def disallowWhenNoBearerToken(authValidationEnabled: Boolean, initialPredicate: Predicate): Unit =
      "disallow users that are not logged in" in new Test {
        mockConfidenceLevelCheckConfig(authValidationEnabled = authValidationEnabled)

        MockedAuthConnector
          .authorised(initialPredicate, retrievals)
          .once()
          .returns(Future.failed(MissingBearerToken()))

        val result: AuthOutcome = await(enrolmentsAuthService.authorised(mtdId, "correlationId", endpointAllowsSupportingAgents = true))
        result shouldBe Left(ClientOrAgentNotAuthorisedError)
      }

    def disallowUsersWithoutEnrolments(authValidationEnabled: Boolean, initialPredicate: Predicate): Unit =
      "disallow users without enrolments" in new Test {
        mockConfidenceLevelCheckConfig(authValidationEnabled = authValidationEnabled)

        MockedAuthConnector
          .authorised(initialPredicate, retrievals)
          .once()
          .returns(Future.failed(InsufficientEnrolments()))

        val result: AuthOutcome = await(enrolmentsAuthService.authorised(mtdId, "correlationId", endpointAllowsSupportingAgents = true))
        result shouldBe Left(ClientOrAgentNotAuthorisedError)
      }
  }

  trait Test {
    val mockAuthConnector: AuthConnector = mock[AuthConnector]

    lazy val enrolmentsAuthService = new EnrolmentsAuthService(mockAuthConnector, mockAppConfig)

    val json = Json.parse("""{ "loginTimes": { "currentLogin": "2015-01-01T12:00:00.000Z", "previousLogin": "2012-01-01T12:00:00.000Z" }}""")

    val loginTimesValue: LoginTimes = Retrievals.loginTimes.reads.reads(json).get

    def getRetrievalsResult(affinity: Option[AffinityGroup], enrolments: Enrolments) =
      affinity and enrolments and None and None and None and None and ConfidenceLevel.L200 and None and None and None and None and AgentInformation(
        None,
        None,
        None) and None and None and None and None and loginTimesValue and None and None

    val retrievals =
      affinityGroup and allEnrolments and internalId and externalId and agentCode and credentials and confidenceLevel and nino and saUtr and dateOfBirth and email and agentInformation and groupIdentifier and credentialRole and mdtpInformation and credentialStrength and loginTimes and itmpName and itmpAddress

    object MockedAuthConnector {

      def authorised[A](predicate: Predicate, retrievals: Retrieval[A]): CallHandler[Future[A]] = {
        (mockAuthConnector
          .authorise[A](_: Predicate, _: Retrieval[A])(_: HeaderCarrier, _: ExecutionContext))
          .expects(predicate, retrievals, *, *)
      }

    }

    def mockConfidenceLevelCheckConfig(authValidationEnabled: Boolean): Unit = {
      MockedAppConfig.confidenceLevelConfig
        .anyNumberOfTimes()
        .returns(
          ConfidenceLevelConfig(
            confidenceLevel = ConfidenceLevel.L200,
            definitionEnabled = true,
            authValidationEnabled = authValidationEnabled
          )
        )
    }

    def getIdentityData(agentInformation: AgentInformation, affinityGroup: Option[AffinityGroup]) = IdentityData(
      None,
      None,
      None,
      None,
      ConfidenceLevel.L200,
      None,
      None,
      None,
      None,
      AgentInformation(None, None, None),
      None,
      None,
      None,
      ItmpName(None, None, None),
      None,
      ItmpAddress(None, None, None, None, None, None, None, None),
      affinityGroup,
      None,
      loginTimesValue
    )

  }

}
