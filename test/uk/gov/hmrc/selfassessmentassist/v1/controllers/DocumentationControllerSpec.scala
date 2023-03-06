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

package uk.gov.hmrc.selfassessmentassist.v1.controllers

import controllers.Assets
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.mvc.Result
import uk.gov.hmrc.selfassessmentassist.mocks.utils.MockCurrentDateTime
import uk.gov.hmrc.selfassessmentassist.v1.mocks.connectors.MockLookupConnector
import uk.gov.hmrc.selfassessmentassist.v1.mocks.requestParsers.MockGenerateReportRequestParser
import uk.gov.hmrc.selfassessmentassist.v1.mocks.services._
import uk.gov.hmrc.selfassessmentassist.v1.mocks.utils.MockIdGenerator

import scala.concurrent.Future

class DocumentationControllerSpec
  extends ControllerBaseSpec
    with MockEnrolmentsAuthService
    with MockLookupConnector
    with MockNrsService
    with MockInsightService
    with MockRdsService
    with MockCurrentDateTime
    with MockIdGenerator
    with MockGenerateReportRequestParser
    with MockIfsService
    with GuiceOneAppPerSuite {

  private val assets = app.injector.instanceOf[Assets]

  trait Test {

    val controller: TestController = new TestController()

    class TestController extends DocumentationController(
      cc = cc,
      assets = assets,
    )

  }

  "DocumentationController" when {
    "definition is OK" should {
      "Return 200" in new Test {
        val result: Future[Result] = controller.definition()(fakePostRequest)
        status(result) shouldBe OK
      }

    }

    "specification is OK" should {
      "Return 200" in new Test {
        val result: Future[Result] = controller.specification("1.0", "docs/overview.md")(fakePostRequest)
        status(result) shouldBe OK
      }

    }

  }
}