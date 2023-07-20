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

package uk.gov.hmrc.selfassessmentassist.api.controllers

import play.api.mvc._
import uk.gov.hmrc.auth.core.Enrolment
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.selfassessmentassist.api.connectors.MtdIdLookupConnector
import uk.gov.hmrc.selfassessmentassist.api.models.auth.UserDetails
import uk.gov.hmrc.selfassessmentassist.api.models.domain.NinoChecker
import uk.gov.hmrc.selfassessmentassist.api.models.errors._
import uk.gov.hmrc.selfassessmentassist.utils.ErrorToJsonConverter.convertErrorAsJson
import uk.gov.hmrc.selfassessmentassist.utils.Logging
import uk.gov.hmrc.selfassessmentassist.v1.services.EnrolmentsAuthService

import scala.concurrent.{ExecutionContext, Future}

case class UserRequest[A](userDetails: UserDetails, request: Request[A]) extends WrappedRequest[A](request)

abstract class AuthorisedController(cc: ControllerComponents)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with ApiBaseController
    with BaseController
    with Logging {

  val authService: EnrolmentsAuthService
  val lookupConnector: MtdIdLookupConnector

  def authorisedAction(nino: String)(implicit correlationId: String): ActionBuilder[UserRequest, AnyContent] =
    new ActionBuilder[UserRequest, AnyContent] {

      override def parser: BodyParser[AnyContent] = cc.parsers.defaultBodyParser

      override protected def executionContext: ExecutionContext = cc.executionContext

      def predicate(mtdId: String): Predicate =
        Enrolment("HMRC-MTD-IT")
          .withIdentifier("MTDITID", mtdId)
          .withDelegatedAuthRule("mtd-it-auth")

      def invokeBlockWithAuthCheck[A](mtdId: String, request: Request[A], block: UserRequest[A] => Future[Result])(implicit
          headerCarrier: HeaderCarrier): Future[Result] = {
        val clientID = request.headers.get("X-Client-Id").getOrElse("N/A")
        authService
          .authorised(predicate(mtdId), correlationId)
          .flatMap[Result] {
            case Right(userDetails) => block(UserRequest(userDetails.copy(clientID = clientID), request))
            case Left(ClientOrAgentNotAuthorisedError) =>
              convertErrorAsJson(ClientOrAgentNotAuthorisedError)
              Future.successful(Forbidden(convertErrorAsJson(ClientOrAgentNotAuthorisedError)))
            case Left(ForbiddenDownstreamError) =>
              logger.warn(s"$correlationId::[invokeBlock]Forbidden downstream error")
              Future.successful(Forbidden(convertErrorAsJson(InternalError)))
            case Left(InvalidBearerTokenError) =>
              Future.successful(Forbidden(convertErrorAsJson(InvalidCredentialsError)))
            case Left(BearerTokenExpiredError) =>
              Future.successful(Forbidden(convertErrorAsJson(InvalidCredentialsError)))
            case Left(LegacyUnauthorisedError) =>
              Future.successful(Forbidden(convertErrorAsJson(LegacyUnauthorisedError)))
            case Left(_) =>
              logger.warn(s"$correlationId::[invokeBlock]Downstream")
              Future.successful(InternalServerError(convertErrorAsJson(InternalError)))
            case _ =>
              logger.error(s"$correlationId::[invokeBlock]Unknown error")
              Future.successful(InternalServerError(convertErrorAsJson(InternalError)))
          }
          .map(_.withApiHeaders(correlationId))
      }

      override def invokeBlock[A](request: Request[A], block: UserRequest[A] => Future[Result]): Future[Result] = {

        implicit val headerCarrier: HeaderCarrier = hc(request)

        if (NinoChecker.isValid(nino)) {
          lookupConnector.getMtdId(nino).flatMap[Result] {
            case Right(mtdId)          => invokeBlockWithAuthCheck(mtdId, request, block)
            case Left(NinoFormatError) =>
              // lookup connector is sending this error instead of forbidden even for valid nino
              logger.error(s"$correlationId::[invokeBlock] MTDID lookup returned NinoFormatError")
              Future.successful(Forbidden(convertErrorAsJson(UnauthorisedError)))
            case Left(UnauthorisedError)       => Future.successful(Forbidden(convertErrorAsJson(UnauthorisedError)))
            case Left(InvalidBearerTokenError) => Future.successful(Unauthorized(convertErrorAsJson(InvalidBearerTokenError)))
            case Left(_)                       => Future.successful(InternalServerError(convertErrorAsJson(InternalError)))
          }
        } else {
          logger.warn(s"$correlationId::[invokeBlock]Error in nino format")
          Future.successful(BadRequest(convertErrorAsJson(NinoFormatError)).withApiHeaders(correlationId))
        }
      }

    }

}

object AuthorisedController {
  val ninoKey: String = "nino"
}
