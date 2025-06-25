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

import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual, Organisation}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.{ItmpAddress, ItmpName, ~}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.selfassessmentassist.api.models.auth.{AuthOutcome, UserDetails}
import uk.gov.hmrc.selfassessmentassist.api.models.errors.{InternalError, MtdError, ClientOrAgentNotAuthorisedError}
import uk.gov.hmrc.selfassessmentassist.config.AppConfig
import uk.gov.hmrc.selfassessmentassist.utils.Logging
import uk.gov.hmrc.selfassessmentassist.v1.models.request.nrs.IdentityData
import uk.gov.hmrc.selfassessmentassist.v1.services.EnrolmentsAuthService._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EnrolmentsAuthService @Inject() (val connector: AuthConnector, val appConfig: AppConfig) extends Logging {

  private lazy val authorisationEnabled = appConfig.confidenceLevelConfig.authValidationEnabled

  private def initialPredicate(mtdId: String): Predicate =
    if (authorisationEnabled)
      authorisationEnabledPredicate(mtdId)
    else
      authorisationDisabledPredicate(mtdId)

  private val authFunction: AuthorisedFunctions = new AuthorisedFunctions {
    override def authConnector: AuthConnector = connector
  }

  def authorised(mtdId: String, correlationId: String, endpointAllowsSupportingAgents: Boolean = false)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): Future[AuthOutcome] = {
    authFunction
      .authorised(initialPredicate(mtdId))
      .retrieve(
        affinityGroup and allEnrolments
          and internalId and externalId and agentCode and credentials
          and confidenceLevel and nino and saUtr and dateOfBirth
          and email and agentInformation and groupIdentifier and credentialRole
          and mdtpInformation and credentialStrength and loginTimes
          and itmpName and itmpAddress) {
        case Some(affGroup) ~ enrolments ~ inId ~ exId ~ agCode ~ creds
            ~ confLevel ~ ni ~ saRef ~ dob
            ~ eml ~ agInfo ~ groupId ~ credRole
            ~ mdtpInfo ~ credStrength ~ logins
            ~ itmpName ~ itmpAddress =>
          val emptyItmpName: ItmpName       = ItmpName(None, None, None)
          val emptyItmpAddress: ItmpAddress = ItmpAddress(None, None, None, None, None, None, None, None)

          val identityData =
            IdentityData(
              inId,
              exId,
              agCode,
              creds,
              confLevel,
              ni,
              saRef,
              dob,
              eml,
              agInfo,
              groupId,
              credRole,
              mdtpInfo,
              itmpName.getOrElse(emptyItmpName),
              itmpDateOfBirth = None,
              itmpAddress.getOrElse(emptyItmpAddress),
              Some(affGroup),
              credStrength,
              logins
            )

          createUserDetailsWithLogging(affinityGroup = affGroup, enrolments, correlationId, Some(identityData), mtdId, endpointAllowsSupportingAgents)
        case _ =>
          logger.warn(s"[EnrolmentsAuthService][authorised] Invalid AffinityGroup.")
          Future.successful(Left(ClientOrAgentNotAuthorisedError))
      }
      .recoverWith {
        case _: MissingBearerToken =>
          Future.successful(Left(ClientOrAgentNotAuthorisedError))
        case _: AuthorisationException =>
          Future.successful(Left(ClientOrAgentNotAuthorisedError))
        case error =>
          logger.warn(s"[EnrolmentsAuthService][authorised] An unexpected error occurred: $error")
          Future.successful(Left(InternalError))
      }
  }

  def createUserDetailsWithLogging(
      affinityGroup: AffinityGroup,
      enrolments: Enrolments,
      correlationId: String,
      identityData: Option[IdentityData],
      mtdId: String,
      endpointAllowsSupportingAgents: Boolean)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[MtdError, UserDetails]] = {
    val clientReference = getClientReferenceFromEnrolments(enrolments)
    logger.debug(
      s"$correlationId::[createUserDetailsWithLogging] Authorisation succeeded as " +
        s"fully-authorised organisation with client reference $clientReference.")

    val userDetails = UserDetails(
      userType = affinityGroup,
      agentReferenceNumber = None,
      clientID = "",
      identityData
    )

    if (affinityGroup != AffinityGroup.Agent) {
      logger.info(s"$correlationId::[createUserDetailsWithLogging] Agent not part of affinityGroup")
      Future.successful(Right(userDetails))
    } else {
      logger.info(s"$correlationId::[createUserDetailsWithLogging] Agent is part of affinityGroup")
      authFunction
        .authorised(mtdEnrolmentPredicate(mtdId)) {
          val agentReferenceNumber = getAgentReferenceFromEnrolments(enrolments)

          agentReferenceNumber match {
            case Right(arn)  => Future.successful(Right(userDetails.copy(agentReferenceNumber = Some(arn))))
            case Left(error) => Future.successful(Left(error))
          }
        }
        .recoverWith { case _: AuthorisationException =>
          if (endpointAllowsSupportingAgents) {
            authFunction
              .authorised(supportingAgentAuthPredicate(mtdId)) {
                val agentReferenceNumber = getAgentReferenceFromEnrolments(enrolments)
                agentReferenceNumber match {
                  case Right(arn)  => Future.successful(Right(userDetails.copy(agentReferenceNumber = Some(arn))))
                  case Left(error) => Future.successful(Left(error))
                }
              }
              .recoverWith { e =>
                Future.successful(Left(ClientOrAgentNotAuthorisedError))
              }
          } else {
            Future.successful(Left(ClientOrAgentNotAuthorisedError))
          }
            .recoverWith { e =>
              Future.successful(Left(ClientOrAgentNotAuthorisedError))
            }
        }
    }
  }

  def getClientReferenceFromEnrolments(enrolments: Enrolments): Option[String] = enrolments
    .getEnrolment("HMRC-MTD-IT")
    .flatMap(_.getIdentifier("MTDITID"))
    .map(_.value)

  private def getAgentReferenceFromEnrolments(enrolments: Enrolments): Either[MtdError, String] =
    (
      for {
        enrolment  <- enrolments.getEnrolment("HMRC-AS-AGENT")
        identifier <- enrolment.getIdentifier("AgentReferenceNumber")
        arn = identifier.value
      } yield arn
    ).toRight(left = {
      logger.warn(s"[EnrolmentsAuthService][authorised] No AgentReferenceNumber defined on agent enrolment.")
      InternalError
    })

}

object EnrolmentsAuthService {

  def authorisationEnabledPredicate(mtdId: String): Predicate =
    (Individual and ConfidenceLevel.L200 and mtdEnrolmentPredicate(mtdId)) or
      (Organisation and mtdEnrolmentPredicate(mtdId)) or
      (Agent and Enrolment("HMRC-AS-AGENT"))

  def authorisationDisabledPredicate(mtdId: String): Predicate =
    mtdEnrolmentPredicate(mtdId) or (Agent and Enrolment("HMRC-AS-AGENT"))

  def mtdEnrolmentPredicate(mtdId: String): Enrolment = {
    Enrolment("HMRC-MTD-IT")
      .withIdentifier("MTDITID", mtdId)
      .withDelegatedAuthRule("mtd-it-auth")
  }

  def supportingAgentAuthPredicate(mtdId: String): Enrolment = {
    Enrolment("HMRC-MTD-IT-SUPP")
      .withIdentifier("MTDITID", mtdId)
      .withDelegatedAuthRule("mtd-it-auth-supp")
  }

}
