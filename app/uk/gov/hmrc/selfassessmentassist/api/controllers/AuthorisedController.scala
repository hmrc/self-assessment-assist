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
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.selfassessmentassist.api.connectors.MtdIdLookupConnector
import uk.gov.hmrc.selfassessmentassist.api.models.auth.UserDetails
import uk.gov.hmrc.selfassessmentassist.api.models.errors._
import uk.gov.hmrc.selfassessmentassist.config.{AppConfig, FeatureSwitch, SupportingAgentsAccessControlEnabled}
import uk.gov.hmrc.selfassessmentassist.v1.services.EnrolmentsAuthService

import scala.concurrent.{ExecutionContext, Future}

case class UserRequest[A](userDetails: UserDetails, request: Request[A]) extends WrappedRequest[A](request)

abstract class AuthorisedController(
    cc: ControllerComponents
)(implicit appConfig: AppConfig, ec: ExecutionContext)
    extends BackendController(cc) {

  val authService: EnrolmentsAuthService
  val lookupConnector: MtdIdLookupConnector

  val endpointName: String

  val featureSwitch: FeatureSwitch = FeatureSwitch(appConfig.featureSwitch)

  lazy private val supportingAgentsAccessControlEnabled: Boolean = featureSwitch.isEnabled(SupportingAgentsAccessControlEnabled)

  lazy private val endpointAllowsSupportingAgents: Boolean = {
    supportingAgentsAccessControlEnabled &&
    appConfig.endpointAllowsSupportingAgents(endpointName)
  }

  def authorisedAction(nino: String): ActionBuilder[UserRequest, AnyContent] = new ActionBuilder[UserRequest, AnyContent] {

    override def parser: BodyParser[AnyContent] = cc.parsers.defaultBodyParser

    override protected def executionContext: ExecutionContext = cc.executionContext

    def invokeBlockWithAuthCheck[A](
        mtdId: String,
        request: Request[A],
        block: UserRequest[A] => Future[Result]
    )(implicit headerCarrier: HeaderCarrier): Future[Result] = {

      authService.authorised(mtdId, endpointAllowsSupportingAgents).flatMap[Result] {
        case Right(userDetails) =>
          block(UserRequest(userDetails.copy(AffinityGroup.Agent, None, clientID = mtdId, None), request))
        case Left(mtdError) =>
          errorResponse(mtdError)
      }
    }

    override def invokeBlock[A](request: Request[A], block: UserRequest[A] => Future[Result]): Future[Result] = {

      implicit val headerCarrier: HeaderCarrier = hc(request)

      lookupConnector.getMtdId(nino).flatMap[Result] {
        case Right(mtdId) =>
          invokeBlockWithAuthCheck(mtdId, request, block)
        case Left(mtdError) =>
          errorResponse(mtdError)
      }
    }

    private def errorResponse[A](mtdError: MtdError): Future[Result] = Future.successful(Status(mtdError.httpStatus)(mtdError.asJson))
  }

}
