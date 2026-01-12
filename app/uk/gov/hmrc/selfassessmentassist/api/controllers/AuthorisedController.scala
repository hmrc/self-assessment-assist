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

import play.api.mvc.*
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.selfassessmentassist.api.connectors.MtdIdLookupConnector
import uk.gov.hmrc.selfassessmentassist.api.models.auth.UserDetails
import uk.gov.hmrc.selfassessmentassist.api.models.domain.NinoChecker
import uk.gov.hmrc.selfassessmentassist.api.models.errors.*
import uk.gov.hmrc.selfassessmentassist.config.{AppConfig, FeatureSwitch}
import uk.gov.hmrc.selfassessmentassist.utils.ErrorToJsonConverter.convertErrorAsJson
import uk.gov.hmrc.selfassessmentassist.utils.Logging
import uk.gov.hmrc.selfassessmentassist.v1.services.EnrolmentsAuthService

import scala.concurrent.{ExecutionContext, Future}

case class UserRequest[A](userDetails: UserDetails, request: Request[A]) extends WrappedRequest[A](request)

abstract class AuthorisedController(cc: ControllerComponents)(implicit appConfig: AppConfig, ec: ExecutionContext)
    extends BackendController(cc)
    with ApiBaseController
    with BaseController
    with Logging {

  val authService: EnrolmentsAuthService
  val lookupConnector: MtdIdLookupConnector

  val endpointName: String

  lazy private val supportingAgentsAccessControlEnabled: Boolean = FeatureSwitch(appConfig.featureSwitch).supportingAgentsAccessControlEnabled

  lazy private val endpointAllowsSupportingAgents: Boolean = {
    supportingAgentsAccessControlEnabled &&
    appConfig.endpointAllowsSupportingAgents(endpointName)
  }

  def authorisedAction(nino: String)(implicit correlationId: String): ActionBuilder[UserRequest, AnyContent] =
    new ActionBuilder[UserRequest, AnyContent] {

      override def parser: BodyParser[AnyContent] = cc.parsers.defaultBodyParser

      override protected def executionContext: ExecutionContext = cc.executionContext

      def invokeBlockWithAuthCheck[A](mtdId: String, request: Request[A], block: UserRequest[A] => Future[Result])(implicit
          headerCarrier: HeaderCarrier): Future[Result] = {
        val clientID = request.headers.get("X-Client-Id").getOrElse("N/A")
        authService
          .authorised(mtdId, correlationId, endpointAllowsSupportingAgents)
          .flatMap[Result] {
            case Right(userDetails) => block(UserRequest(userDetails.copy(clientID = clientID), request))

            case Left(mtdError) =>
              errorResponse(mtdError)
          }
          .map(_.withApiHeaders(correlationId))
      }

      override def invokeBlock[A](request: Request[A], block: UserRequest[A] => Future[Result]): Future[Result] = {

        implicit val headerCarrier: HeaderCarrier = hc(request)

        if (NinoChecker.isValid(nino)) {
          lookupConnector.getMtdId(nino).flatMap[Result] {
            case Right(mtdId) => invokeBlockWithAuthCheck(mtdId, request, block)
            case Left(mtdError) =>
              val lookupMtdError: MtdError = mtdError.httpStatus match {
                case FORBIDDEN    => ClientOrAgentNotAuthorisedError
                case UNAUTHORIZED => InvalidCredentialsError
                case _            => InternalError
              }
              errorResponse(lookupMtdError)
          }
        } else {
          logger.warn(s"$correlationId::[invokeBlock]Error in nino format")
          Future.successful(BadRequest(convertErrorAsJson(NinoFormatError)).withApiHeaders(correlationId))
        }
      }

      private def errorResponse[A](mtdError: MtdError): Future[Result] = Future.successful(Status(mtdError.httpStatus)(mtdError.asJson))

    }

}

object AuthorisedController {
  val ninoKey: String = "nino"
}
