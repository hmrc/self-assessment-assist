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

package routing

import com.typesafe.config.ConfigFactory
import org.apache.pekko.actor.ActorSystem
import org.scalatest.Inside
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import play.api.http.{HttpConfiguration, HttpErrorHandler, HttpFilters}
import play.api.mvc.*
import play.api.routing.Router
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.selfassessmentassist.api.models.errors.{InvalidAcceptHeaderError, UnsupportedVersionError}
import uk.gov.hmrc.selfassessmentassist.routing.{VersionRoutingMap, VersionRoutingRequestHandler}
import uk.gov.hmrc.selfassessmentassist.support.{MockAppConfig, UnitSpec}

class VersionRoutingRequestHandlerSpec extends UnitSpec with Inside with MockAppConfig with GuiceOneAppPerSuite {
  test =>

  implicit private val actorSystem: ActorSystem = ActorSystem("test")
  val action: DefaultActionBuilder              = app.injector.instanceOf[DefaultActionBuilder]

  import play.api.mvc.Handler
  import play.api.routing.sird._

  object DefaultHandler extends Handler
  object V1Handler      extends Handler
  object V2Handler      extends Handler
  object V3Handler      extends Handler

  private val defaultRouter = Router.from { case POST(p"") =>
    DefaultHandler
  }

  private val v1Router = Router.from { case POST(p"/reports/AA000000B/111190b4-06e3-4fef-a555-6fd0877dc7ca") =>
    V1Handler
  }

  private val v2Router = Router.from { case POST(p"/reports/AA000000B/111190b4-06e3-4fef-a555-6fd0877dc7ca") =>
    V2Handler
  }

  private val v3Router = Router.from { case POST(p"/reports/AA000000B/111190b4-06e3-4fef-a555-6fd0877dc7ca") =>
    V3Handler
  }

  private val routingMap = new VersionRoutingMap {
    override val defaultRouter: Router    = test.defaultRouter
    override val map: Map[String, Router] = Map("1.0" -> v1Router, "2.0" -> v2Router, "3.0" -> v3Router)
  }

  class Test(implicit acceptHeader: Option[String]) {
    val httpConfiguration: HttpConfiguration = HttpConfiguration("context")
    private val errorHandler                 = mock[HttpErrorHandler]
    private val filters                      = mock[HttpFilters]
    (() => filters.filters).stubs().returns(Nil)

    MockedAppConfig.featureSwitch.returns(Some(Configuration(ConfigFactory.parseString("""
                                                                                         |version-1.enabled = true
                                                                                         |version-2.enabled = true
                                                                         """.stripMargin))))

    val requestHandler: VersionRoutingRequestHandler =
      new VersionRoutingRequestHandler(routingMap, errorHandler, httpConfiguration, mockAppConfig, filters, action)

    def buildRequest(path: String): RequestHeader =
      acceptHeader
        .foldLeft(FakeRequest("POST", path)) { (req, accept) =>
          req.withHeaders((ACCEPT, accept))
        }

  }

  "Routing requests with no version" should {
    implicit val acceptHeader: None.type = None

    handleWithDefaultRoutes()
  }

  "Routing requests with valid version" should {
    implicit val acceptHeader: Some[String] = Some("application/vnd.hmrc.1.0+json")

    handleWithDefaultRoutes()
  }

  "Routing requests to non default router with no version" should {
    implicit val acceptHeader: None.type = None

    "return 406" in new Test {

      val request: RequestHeader = buildRequest("/reports/AA000000B/111190b4-06e3-4fef-a555-6fd0877dc7ca")
      inside(requestHandler.routeRequest(request)) { case Some(b: EssentialAction) =>
        val result = b.apply(request)

        status(result) shouldBe NOT_ACCEPTABLE
        contentAsJson(result) shouldBe InvalidAcceptHeaderError.asJson
      }
    }
  }

  "Routing requests with v1" should {
    implicit val acceptHeader: Some[String] = Some("application/vnd.hmrc.1.0+json")
    handleWithVersionRoutes("/reports/AA000000B/111190b4-06e3-4fef-a555-6fd0877dc7ca", V1Handler)
  }

  "Routing requests with v2" should {
    implicit val acceptHeader: Some[String] = Some("application/vnd.hmrc.2.0+json")
    handleWithVersionRoutes("/reports/AA000000B/111190b4-06e3-4fef-a555-6fd0877dc7ca", V2Handler)
  }

  "Routing requests with unsupported version" should {
    implicit val acceptHeader: Some[String] = Some("application/vnd.hmrc.5.0+json")

    "return 406" in new Test {
      private val request = buildRequest("/reports/AA000000B/111190b4-06e3-4fef-a555-6fd0877dc7ca")

      inside(requestHandler.routeRequest(request)) { case Some(b: EssentialAction) =>
        val result = b.apply(request)

        status(result) shouldBe NOT_ACCEPTABLE
        contentAsJson(result) shouldBe InvalidAcceptHeaderError.asJson
      }
    }
  }

  "Routing requests for supported version but not enabled" when {
    implicit val acceptHeader: Some[String] = Some("application/vnd.hmrc.3.0+json")

    "the version has a route for the resource" must {
      "return 404 Not Found" in new Test {

        private val request = buildRequest("/reports/AA000000B/111190b4-06e3-4fef-a555-6fd0877dc7ca")
        inside(requestHandler.routeRequest(request)) { case Some(b: EssentialAction) =>
          val result = b.apply(request)

          status(result) shouldBe NOT_FOUND
          contentAsJson(result) shouldBe UnsupportedVersionError.asJson

        }
      }
    }
  }

  private def handleWithDefaultRoutes()(implicit acceptHeader: Option[String]): Unit = {
    "if the request ends with a trailing slash" when {
      "handler found" should {
        "use it" in new Test {
          requestHandler.routeRequest(buildRequest("/")) shouldBe Some(DefaultHandler)
        }
      }

      "handler not found" should {
        "try without the trailing slash" in new Test {

          requestHandler.routeRequest(buildRequest("")) shouldBe Some(DefaultHandler)
        }
      }
    }
  }

  private def handleWithVersionRoutes(path: String, handler: Handler)(implicit acceptHeader: Option[String]): Unit = {
    "if the request ends with a trailing slash" when {
      "handler found" should {
        "use it" in new Test {

          requestHandler.routeRequest(buildRequest(s"$path/")) shouldBe Some(handler)
        }
      }

      "handler not found" should {
        "try without the trailing slash" in new Test {

          requestHandler.routeRequest(buildRequest(s"$path")) shouldBe Some(handler)
        }
      }
    }
  }

}
