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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.transactionalrisking.connectors.GreetingConnector
import uk.gov.hmrc.transactionalrisking.models.Greeting

import scala.concurrent.Future

class HelloWorldControllerSpec extends AnyWordSpec with MockitoSugar with Matchers {

  "The hello world controller" when {
    "actioned with GET /hello" should {
      "reply 200 and the greeting message" in {
        // SETUP
        // Create the mock connector and instruct it on what to do
        val connector = mock[GreetingConnector]
        when(connector.getGreeting()(any())).thenReturn(Future.successful(Greeting("ciao mondo!")))

        // Configure the DI - Dependency Injection providers
        val app = new GuiceApplicationBuilder()
          .overrides(
            bind[GreetingConnector].toInstance(connector)
          )
          .build()

        // Ask the DI injector to provide an instance of the "thing under test"
        val controller = app.injector.instanceOf[HelloWorldController]
        val fakeRequest = FakeRequest("GET", "/hello")

        // EXERCISE
        val result = controller.hello()(fakeRequest)

        // VERIFY
        status(result) shouldBe Status.OK
        contentAsString(result) shouldBe "ciao mondo!"
        verify(connector, times(1)).getGreeting()(any())
      }
    }
  }
}
