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

package uk.gov.hmrc.transactionalrisking.v1.mocks.requestParsers

import org.scalamock.handlers.{CallHandler}
import org.scalamock.scalatest.MockFactory
import uk.gov.hmrc.transactionalrisking.controllers.requestParsers.AcknowledgeRequestParser
import uk.gov.hmrc.transactionalrisking.models.request.AcknowledgeReportRawData
import uk.gov.hmrc.transactionalrisking.services.{ParseOutcome}
import uk.gov.hmrc.transactionalrisking.services.nrs.models.request.AcknowledgeReportRequest
import uk.gov.hmrc.transactionalrisking.v1.TestData.CommonTestData.commonTestData.{internalCorrelationIDImplicit, simpleNino, simpleRDSCorrelationID, simpleReportID}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

trait MockAcknowledgeRequestParser extends MockFactory {

  val mockAcknowledgeRequestParser: AcknowledgeRequestParser = mock[AcknowledgeRequestParser]

  object MockAcknowledgeRequestParser {

    def parseRequest(rawData: AcknowledgeReportRawData): CallHandler[Future[ParseOutcome[AcknowledgeReportRequest]]] = {
      (mockAcknowledgeRequestParser.parseRequest(_: AcknowledgeReportRawData)(_:ExecutionContext, _: String)).expects(*,*, *).anyNumberOfTimes() returns
        (Future(Right(AcknowledgeReportRequest(simpleNino, simpleReportID.toString, simpleRDSCorrelationID))))
    }
  }

}
