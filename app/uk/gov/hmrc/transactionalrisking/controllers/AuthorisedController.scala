/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.transactionalrisking.controllers

import play.api.libs.json.Json
import play.api.mvc._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.{Enrolment, Nino}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.transactionalrisking.controllers.AuthorisedController.ninoKey
import uk.gov.hmrc.transactionalrisking.models.auth.UserDetails
import uk.gov.hmrc.transactionalrisking.models.domain.NinoChecker
import uk.gov.hmrc.transactionalrisking.models.errors.{ClientOrAgentNotAuthorisedError, DownstreamError, ForbiddenDownstreamError, NinoFormatError}
import uk.gov.hmrc.transactionalrisking.services.EnrolmentsAuthService

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.transactionalrisking.utils.Logging

case class UserRequest[A](userDetails: UserDetails, request: Request[A]) extends WrappedRequest[A](request)

abstract class AuthorisedController(cc: ControllerComponents)(implicit ec: ExecutionContext) extends BackendController(cc) with Logging {

  val authService: EnrolmentsAuthService

  def authorisedAction(nino: String, correlationID: String, nrsRequired: Boolean = false): ActionBuilder[UserRequest, AnyContent] = new ActionBuilder[UserRequest, AnyContent] {
    logger.info(s"$correlationID::[authorisedAction] Check we have authority to do the required work and do it if possible.")

    override def parser: BodyParser[AnyContent] = cc.parsers.defaultBodyParser

    override protected def executionContext: ExecutionContext = cc.executionContext

    //TODO fix predicate, maybe we need to use delegatedAuthRule("sa-auth"), but this was failing
    //https://confluence.tools.tax.service.gov.uk/display/GG/Predicate+Reference
    //https://confluence.tools.tax.service.gov.uk/display/AG/Agent++Access+Control+-+patterns
    //https://confluence.tools.tax.service.gov.uk/display/AG/2.+Self+Assessment+Agent+Access+Control
    //https://confluence.tools.tax.service.gov.uk/display/AG/Integration+with+sa-auth


    def predicate(nino: String): Predicate =
      Nino(hasNino = true, nino = Some(nino)) or Enrolment("IR-SA").withIdentifier(ninoKey, nino)
    .withDelegatedAuthRule("sa-auth")

    override def invokeBlock[A](request: Request[A], block: UserRequest[A] => Future[Result]): Future[Result] = {
      logger.info(s"$correlationID::[invokeBlock]Check authorisation and continue if ok otherwise some sort of error: $correlationID")

      implicit val headerCarrier: HeaderCarrier = hc(request)
      val clientID = request.headers.get("X-Client-Id").getOrElse("N/A")

      if (NinoChecker.isValid(nino)) {
        authService.authorised(predicate(nino), correlationID, nrsRequired).flatMap[Result] {
          case Right(userDetails) =>
            logger.info(s"$correlationID::[invokeBlock]Nino correct and authorised")
            block(UserRequest(userDetails.copy(clientID = clientID), request))
          case Left(ClientOrAgentNotAuthorisedError) =>
            logger.warn(s"$correlationID::[invokeBlock]Unable to authorise client or agent")
            Future.successful(Forbidden(Json.toJson(ClientOrAgentNotAuthorisedError)))
          case Left(ForbiddenDownstreamError) =>
            logger.warn(s"$correlationID::[invokeBlock]Forbidden downstream error")
            Future.successful(Forbidden(Json.toJson(DownstreamError)))
          case Left(_) =>
            logger.warn(s"$correlationID::[invokeBlock]Downstream")
            Future.successful(InternalServerError(Json.toJson(DownstreamError)))
          case _ =>
            logger.error(s"$correlationID::[invokeBlock]Unknown error")
            Future.successful(InternalServerError(Json.toJson(DownstreamError)))
        }
      } else {
        logger.warn(s"$correlationID::[invokeBlock]Error in nino format")
        Future.successful(BadRequest(Json.toJson(NinoFormatError)))
      }
    }
  }
}

object AuthorisedController {
  val ninoKey: String = "nino"
}
