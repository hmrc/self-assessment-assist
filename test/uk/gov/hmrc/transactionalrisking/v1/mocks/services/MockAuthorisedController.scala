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

package uk.gov.hmrc.transactionalrisking.v1.mocks.services

import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import play.api.mvc.{ActionBuilder, AnyContent}
import uk.gov.hmrc.transactionalrisking.controllers.{AuthorisedController, UserRequest}

trait MockAuthorisedController extends MockFactory {

  val mockAuthorisedController: AuthorisedController = mock[AuthorisedController]

  object MockAuthorisedController {

    def authorisedAction(nino: String, nrsRequired: Boolean = false): CallHandler[ActionBuilder[UserRequest, AnyContent]] = {

      (mockAuthorisedController.authorisedAction(_: String, _: Boolean))
        .expects(*, *)
    }
  }
}