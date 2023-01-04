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

package uk.gov.hmrc.transactionalrisking.v1.controllers

import play.api.libs.json.Json
import play.api.mvc._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.{Enrolment, Nino}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.transactionalrisking.utils.Logging
import uk.gov.hmrc.transactionalrisking.v1.controllers.AuthorisedController.ninoKey
import uk.gov.hmrc.transactionalrisking.v1.models.auth.UserDetails
import uk.gov.hmrc.transactionalrisking.v1.models.domain.NinoChecker
import uk.gov.hmrc.transactionalrisking.v1.models.errors.{BearerTokenExpiredError, ClientOrAgentNotAuthorisedError, DownstreamError, ForbiddenDownstreamError, InvalidBearerTokenError, InvalidCredentialsError, LegacyUnauthorisedError, NinoFormatError}
import uk.gov.hmrc.transactionalrisking.v1.services.EnrolmentsAuthService

import scala.concurrent.{ExecutionContext, Future}

case class UserRequest[A](userDetails: UserDetails, request: Request[A]) extends WrappedRequest[A](request)

abstract class AuthorisedController(cc: ControllerComponents)(implicit ec: ExecutionContext) extends BackendController(cc) with BaseController with Logging {

  val authService: EnrolmentsAuthService

  def authorisedAction(nino: String, nrsRequired: Boolean = false) (implicit correlationId:String): ActionBuilder[UserRequest, AnyContent] = new ActionBuilder[UserRequest, AnyContent] {

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
      implicit val headerCarrier: HeaderCarrier = hc(request)
      val clientID = request.headers.get("X-Client-Id").getOrElse("N/A")

      if (NinoChecker.isValid(nino)) {
        authService.authorised(predicate(nino), correlationId, nrsRequired).flatMap[Result] {
          case Right(userDetails) =>
            block(UserRequest(userDetails.copy(clientID = clientID), request))
          case Left(ClientOrAgentNotAuthorisedError) =>
            Future.successful(Forbidden(Json.toJson(ClientOrAgentNotAuthorisedError)))
          case Left(ForbiddenDownstreamError) =>
            logger.warn(s"$correlationId::[invokeBlock]Forbidden downstream error")
            Future.successful(Forbidden(Json.toJson(DownstreamError)))
          case Left(InvalidBearerTokenError) =>
            Future.successful(Forbidden(Json.toJson(InvalidCredentialsError)))
          case Left(BearerTokenExpiredError) =>
            Future.successful(Forbidden(Json.toJson(InvalidCredentialsError)))
          case Left(LegacyUnauthorisedError) =>
            Future.successful(Forbidden(Json.toJson(LegacyUnauthorisedError)))
          case Left(_) =>
            logger.warn(s"$correlationId::[invokeBlock]Downstream")
            Future.successful(InternalServerError(Json.toJson(DownstreamError)))
          case _ =>
            logger.error(s"$correlationId::[invokeBlock]Unknown error")
            Future.successful(InternalServerError(Json.toJson(DownstreamError)))
        }.map(_.withApiHeaders(correlationId))
      } else {
        logger.warn(s"$correlationId::[invokeBlock]Error in nino format")
        Future.successful(BadRequest(Json.toJson(NinoFormatError)).withApiHeaders(correlationId))
      }
    }
  }
}

object AuthorisedController {
  val ninoKey: String = "nino"
}
