/*
 * Copyright 2026 HM Revenue & Customs
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

import play.api.mvc.{Result, Results}
import uk.gov.hmrc.selfassessmentassist.api.models.errors.{ErrorWrapper, InternalError}
import uk.gov.hmrc.selfassessmentassist.support.UnitSpec
import uk.gov.hmrc.selfassessmentassist.utils.Logging

class BaseControllerSpec extends UnitSpec with Logging {

  object TestController extends BaseController with Logging {

    def callUnhandledError(errorWrapper: ErrorWrapper)(implicit ctx: EndpointLogContext): Result =
      unhandledError(errorWrapper)

  }

  "BaseController Response.withApiHeaders" should {
    "add X-CorrelationId header" in {
      import TestController._

      val result = Results.Ok

      val updated = result.withApiHeaders("corr-id-123")

      updated.header
        .headers("X-CorrelationId")
        .shouldBe("corr-id-123")
    }
  }

  "BaseController.unhandledError" should {
    "return InternalServerError with InternalError JSON" in {

      implicit val ctx: EndpointLogContext =
        EndpointLogContext("BaseControllerSpec", "unhandled-error")

      val result =
        TestController.callUnhandledError(
          ErrorWrapper("corr-id", InternalError)
        )

      result.header.status
        .shouldBe(Results.InternalServerError.header.status)
    }
  }

}
