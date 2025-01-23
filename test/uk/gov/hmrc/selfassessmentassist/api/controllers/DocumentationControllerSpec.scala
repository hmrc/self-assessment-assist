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

import controllers.Assets
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.mvc.Result
import uk.gov.hmrc.selfassessmentassist.definitions.ApiDefinitionFactory

import scala.concurrent.Future

class DocumentationControllerSpec extends ControllerBaseSpec with GuiceOneAppPerSuite {

  "DocumentationController" when {
    "definition is OK" should {
      "Return 200" in new Test {
        val result: Future[Result] = controller.definition()(fakePostRequest)
        status(result) shouldBe OK
      }

    }

    "/file endpoint" should {
      "return a file" in new Test {
        val response: Future[Result] = requestAsset("application.yaml")
        status(response) shouldBe OK
        await(response).body.contentLength.getOrElse(-99L) should be > 0L
      }
    }

  }

  trait Test {

    protected def requestAsset(filename: String, accept: String = "text/yaml"): Future[Result] =
      controller.specification("1.0", filename)(fakePostRequest)

    private val apiFactory = app.injector.instanceOf[ApiDefinitionFactory]

    private val assets       = app.injector.instanceOf[Assets]
    protected val controller = new DocumentationController(apiFactory, assets, cc)
  }

}
