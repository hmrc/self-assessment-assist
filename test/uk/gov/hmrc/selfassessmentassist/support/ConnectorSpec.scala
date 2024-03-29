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

package uk.gov.hmrc.selfassessmentassist.support

import play.api.http.{HeaderNames, MimeTypes, Status}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.selfassessmentassist.api.TestData.CommonTestData

import scala.concurrent.ExecutionContext

trait ConnectorSpec extends UnitSpec with Status with MimeTypes with HeaderNames {

  lazy val baseUrl                  = "http://test-BaseUrl"
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

  implicit val correlationId: String = CommonTestData.correlationId

  val otherHeaders: Seq[(String, String)] = Seq(
    "Gov-Test-Scenario" -> "DEFAULT",
    "AnotherHeader"     -> "HeaderValue"
  )

  val dummyHeaderCarrierConfig: HeaderCarrier.Config =
    HeaderCarrier.Config(
      Seq("^not-test-BaseUrl?$".r),
      Seq.empty[String],
      Some("self-assessment-assist-api")
    )

  implicit val headerCarrier: HeaderCarrier = HeaderCarrier()
  // implicit val hc: HeaderCarrier = HeaderCarrier(requestId = Some(RequestId("123")), otherHeaders = otherHeaders)
}
