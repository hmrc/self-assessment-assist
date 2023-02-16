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

package uk.gov.hmrc.selfassessmentassist.v1.mocks.requestParsers

import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import uk.gov.hmrc.selfassessmentassist.v1.controllers.requestParsers.GenerateReportRequestParser
import uk.gov.hmrc.selfassessmentassist.v1.models.request.GenerateReportRawData
import uk.gov.hmrc.selfassessmentassist.v1.services.ParseOutcome
import uk.gov.hmrc.selfassessmentassist.v1.TestData.CommonTestData._
import uk.gov.hmrc.selfassessmentassist.v1.models.domain.AssessmentRequestForSelfAssessment
import uk.gov.hmrc.selfassessmentassist.v1.models.errors.{ErrorWrapper, MtdError}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

trait MockGenerateReportRequestParser extends MockFactory{

  val mockGenerateReportRequestParser: GenerateReportRequestParser = mock[GenerateReportRequestParser]

  object MockGenerateReportRequestParser {

    def parseRequest(rawData: GenerateReportRawData): CallHandler[Future[ParseOutcome[AssessmentRequestForSelfAssessment]]] = {
      (mockGenerateReportRequestParser.parseRequest(_: GenerateReportRawData)(_:ExecutionContext, _:String)).expects(*,*,*).anyNumberOfTimes() returns
        (Future(Right(AssessmentRequestForSelfAssessment(simpleCalculationId,simpleNino,simplePreferredLanguage,simpleCustomerType,simpleAgentRef,simpleTaxYear))))
    }

    def parseRequestFail(rawData: GenerateReportRawData, mtdError: MtdError): CallHandler[Future[ParseOutcome[AssessmentRequestForSelfAssessment]]] = {
      (mockGenerateReportRequestParser.parseRequest(_: GenerateReportRawData)(_:ExecutionContext, _:String)).expects(*,*,*).anyNumberOfTimes() returns
        (Future(Left(ErrorWrapper(simpleTaxYear, mtdError))))
    }
  }

}
