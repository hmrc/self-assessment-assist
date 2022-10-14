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
import uk.gov.hmrc.auth.core.{Enrolment, Nino}
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.transactionalrisking.models.errors.ForbiddenDownstreamError
import uk.gov.hmrc.transactionalrisking.models.auth.UserDetails
import uk.gov.hmrc.transactionalrisking.models.domain.NinoChecker
import uk.gov.hmrc.transactionalrisking.models.errors.{ClientOrAgentNotAuthorisedError, DownstreamError, ForbiddenDownstreamError, NinoFormatError}
import uk.gov.hmrc.transactionalrisking.services.EnrolmentsAuthService

import scala.concurrent.{ExecutionContext, Future}

case class UserRequest[A](userDetails: UserDetails, request: Request[A]) extends WrappedRequest[A](request)

abstract class AuthorisedController(cc: ControllerComponents)(implicit ec: ExecutionContext) extends BackendController(cc) {

  val authService: EnrolmentsAuthService

  def authorisedAction(nino: String, nrsRequired: Boolean = false): ActionBuilder[UserRequest, AnyContent] = new ActionBuilder[UserRequest, AnyContent] {

    override def parser: BodyParser[AnyContent] = cc.parsers.defaultBodyParser

    override protected def executionContext: ExecutionContext = cc.executionContext

    //TODO fix predicate
    def predicate(nino: String): Predicate =
      Nino(hasNino = true, nino = Some(nino)) or Enrolment("IR-SA").withIdentifier(AuthorisedController.ninoKey, nino)
        //.withDelegatedAuthRule("afi-auth")

    override def invokeBlock[A](request: Request[A], block: UserRequest[A] => Future[Result]): Future[Result] = {

      implicit val headerCarrier: HeaderCarrier = hc(request)

      val clientId = request.headers.get("X-Client-Id").getOrElse("N/A")

      //TODO check if we need to perform NINO validation here Nino.isValid(nino)
      if (NinoChecker.isValid(nino)) {
        authService.authorised(predicate(nino), nrsRequired).flatMap[Result] {
          case Right(userDetails) =>
            block(UserRequest(userDetails.copy(clientId = clientId), request))
          case Left(ClientOrAgentNotAuthorisedError) =>
            Future.successful(Forbidden(Json.toJson(ClientOrAgentNotAuthorisedError)))
          case Left(ForbiddenDownstreamError) =>
            Future.successful(Forbidden(Json.toJson(DownstreamError)))
          case Left(_) =>
            Future.successful(InternalServerError(Json.toJson(DownstreamError)))
        }
      } else {
        Future.successful(BadRequest(Json.toJson(NinoFormatError)))
      }
    }
  }
}

object AuthorisedController {
  val ninoKey: String = "NINO"
}
