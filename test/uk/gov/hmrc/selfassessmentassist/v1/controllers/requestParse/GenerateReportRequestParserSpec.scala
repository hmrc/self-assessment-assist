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

package uk.gov.hmrc.selfassessmentassist.v1.controllers.requestParse

import uk.gov.hmrc.selfassessmentassist.support.UnitSpec
import uk.gov.hmrc.selfassessmentassist.v1.TestData.CommonTestData._
import uk.gov.hmrc.selfassessmentassist.v1.controllers.requestParsers.GenerateReportRequestParser
import uk.gov.hmrc.selfassessmentassist.v1.controllers.requestParsers.validators.GenerateReportValidator
import uk.gov.hmrc.selfassessmentassist.v1.models.domain.{AssessmentRequestForSelfAssessment, CustomerType, PreferredLanguage}
import uk.gov.hmrc.selfassessmentassist.v1.models.errors.MtdError
import uk.gov.hmrc.selfassessmentassist.v1.models.request.{GenerateReportRawData, RawData}

import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

class GenerateReportRequestParserSpec extends UnitSpec {

  case class Raw(nino: String) extends RawData
  case class Request(nino: String)

  trait Test {
    test =>
    val validatorMock = mock[GenerateReportValidator]
    (validatorMock.validate(_: GenerateReportRawData)).expects(*).anyNumberOfTimes().returns(List.empty[MtdError])
  }

  "parse" should {
    "return a Request" when {
      "the validator returns no errors" in new Test {

        val parser: GenerateReportRequestParser = new GenerateReportRequestParser(validatorMock)
        val result = await(
          parser.parseRequest(GenerateReportRawData("0f14d0ab-9605-4a62-a9e4-5ed26688389b", "", PreferredLanguage.English, CustomerType.Agent, None, ""))(implicitly[ExecutionContext], correlationId)
        )
        result shouldBe Right(AssessmentRequestForSelfAssessment(
          UUID.fromString("0f14d0ab-9605-4a62-a9e4-5ed26688389b"),
          "",
          PreferredLanguage.English,
          CustomerType.Agent,
          None,
          ""
        ))
      }
    }
  }
}
