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

package uk.gov.hmrc.transactionalrisking.v1.services

import play.api.libs.json.JsResultException
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual, Organisation}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.{ItmpAddress, ItmpName, ~}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.transactionalrisking.utils.Logging
import uk.gov.hmrc.transactionalrisking.v1.controllers.AuthorisedController
import uk.gov.hmrc.transactionalrisking.v1.models.auth.{AuthOutcome, UserDetails}
import uk.gov.hmrc.transactionalrisking.v1.models.errors.{BearerTokenExpiredError, DownstreamError, ForbiddenDownstreamError, InvalidBearerTokenError, LegacyUnauthorisedError, MtdError}
import uk.gov.hmrc.transactionalrisking.v1.services.nrs.models.request.IdentityData

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EnrolmentsAuthService @Inject()(val connector: AuthConnector) extends Logging {

  private val authFunction: AuthorisedFunctions = new AuthorisedFunctions {
    override def authConnector: AuthConnector = connector
  }

  def authorised(predicate: Predicate, correlationId: String, nrsRequired: Boolean = false)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[AuthOutcome] = {

    if (!nrsRequired) {
      logger.info(s"$correlationId::[authorised]Performing nrs not required")
      authFunction.authorised(predicate).retrieve(affinityGroup and allEnrolments) {
        case Some(Individual) ~ enrolments => createUserDetailsWithLogging(affinityGroup = Individual, enrolments, correlationId)
        case Some(Organisation) ~ enrolments => createUserDetailsWithLogging(affinityGroup = Organisation, enrolments, correlationId)
        case Some(Agent) ~ enrolments => createUserDetailsWithLogging(affinityGroup = Agent, enrolments, correlationId)
        case _ =>
          logger.warn(s"$correlationId::[authorised]Authorisation failed due to unsupported affinity group.")
          Future.successful(Left(LegacyUnauthorisedError))
      } recoverWith unauthorisedError(correlationId)
    } else {
      logger.info(s"$correlationId::[authorised]authorisation NRS required.")

      authFunction.authorised(predicate).retrieve(affinityGroup and allEnrolments
        and internalId and externalId and agentCode and credentials
        and confidenceLevel and nino and saUtr and name and dateOfBirth
        and email and agentInformation and groupIdentifier and credentialRole
        and mdtpInformation and credentialStrength and loginTimes
        and itmpName and itmpAddress
      ) {
        case Some(affGroup) ~ enrolments ~ inId ~ exId ~ agCode ~ creds
          ~ confLevel ~ ni ~ saRef ~ nme ~ dob
          ~ eml ~ agInfo ~ groupId ~ credRole
          ~ mdtpInfo ~ credStrength ~ logins
          ~ itmpName ~ itmpAddress =>

          val emptyItmpName: ItmpName = ItmpName(None, None, None)
          val emptyItmpAddress: ItmpAddress = ItmpAddress(None, None, None, None, None, None, None, None)

          val identityData =
            IdentityData(
              inId, exId, agCode, creds,
              confLevel, ni, saRef, nme, dob,
              eml, agInfo, groupId,
              credRole, mdtpInfo, itmpName.getOrElse(emptyItmpName), itmpDateOfBirth = None,
              itmpAddress.getOrElse(emptyItmpAddress),
              Some(affGroup),
              credStrength,
              logins
            )

          createUserDetailsWithLogging(affinityGroup = affGroup, enrolments, correlationId, Some(identityData))
        case _ =>
          logger.warn(s"$correlationId::[EnrolmentsAuthService] [authorised with nrsRequired = true] Authorisation failed due to unsupported affinity group.")
          Future.successful(Left(LegacyUnauthorisedError))

      } recoverWith unauthorisedError(correlationId)
    }

  }

  private def createUserDetailsWithLogging(affinityGroup: AffinityGroup,
                                           enrolments: Enrolments,
                                           correlationId: String,
                                           identityData: Option[IdentityData] = None): Future[Right[MtdError, UserDetails]] = {
    //TODO Fixme clientReference is coming as none in logs
    val clientReference = getClientReferenceFromEnrolments(enrolments)
    logger.debug(s"$correlationId::[createUserDetailsWithLogging] Authorisation succeeded as " +
      s"fully-authorised organisation with reference $clientReference.")

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
      Future.successful(Right(userDetails.copy(agentReferenceNumber = getAgentReferenceFromEnrolments(enrolments))))
    }
  }

  private def getClientReferenceFromEnrolments(enrolments: Enrolments): Option[String] = enrolments
    .getEnrolment("IR-SA")
    .flatMap(_.getIdentifier(AuthorisedController.ninoKey))
    .map(_.value)

  private def getAgentReferenceFromEnrolments(enrolments: Enrolments): Option[String] = enrolments
    .getEnrolment("IR-SA")
    .flatMap(_.getIdentifier("AgentReferenceNumber"))
    .map(_.value)

  private def unauthorisedError(correlationId: String): PartialFunction[Throwable, Future[AuthOutcome]] = {
    case _: InsufficientEnrolments =>
      logger.warn(s"$correlationId::[unauthorisedError] Client authorisation failed due to unsupported insufficient enrolments.")
      Future.successful(Left(LegacyUnauthorisedError))
    case _: InsufficientConfidenceLevel =>
      logger.warn(s"$correlationId::[unauthorisedError] Client authorisation failed due to unsupported insufficient confidenceLevels.")
      Future.successful(Left(LegacyUnauthorisedError))
    case _: JsResultException =>
      logger.warn(s"$correlationId::[unauthorisedError] - Did not receive minimum data from Auth required for NRS Submission")
      Future.successful(Left(ForbiddenDownstreamError))
    case _: InvalidBearerToken =>
      logger.warn(s"$correlationId::[unauthorisedError] - Invalid Bearer token")
      Future.successful(Left(InvalidBearerTokenError))
    case _: BearerTokenExpired =>
      logger.warn(s"$correlationId::[unauthorisedError] - BearerTokenExpired")
      Future.successful(Left(BearerTokenExpiredError))
    case exception@_ =>
      logger.warn(s"$correlationId::[unauthorisedError] Client authorisation failed due to internal server error. auth-client exception was ${exception.getClass.getSimpleName}")
      Future.successful(Left(DownstreamError))
  }
}
