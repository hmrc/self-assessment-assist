package uk.gov.hmrc.transactionalrisking.v1.mocks.requestParsers

import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import uk.gov.hmrc.transactionalrisking.controllers.requestParsers.AcknowledgeRequestParser
import uk.gov.hmrc.transactionalrisking.models.outcomes.ResponseWrapper
import uk.gov.hmrc.transactionalrisking.models.request.AcknowledgeReportRawData
import uk.gov.hmrc.transactionalrisking.services.ServiceOutcome
import uk.gov.hmrc.transactionalrisking.services.nrs.models.request.AcknowledgeReportRequest
import uk.gov.hmrc.transactionalrisking.v1.CommonTestData.{internalCorrelationId, simpleCorrelationId, simpleNino, simpleReportId}

trait MockAcknowledgeRequestParser extends MockFactory {

  val mockAcknowledgeRequestParser: AcknowledgeRequestParser = mock[AcknowledgeRequestParser]

  object MockAcknowledgeRequestParser {

    def parseRequest(rawData: AcknowledgeReportRawData): CallHandler[ServiceOutcome[AcknowledgeReportRequest]] = {
      (mockAcknowledgeRequestParser.parseRequest(_: AcknowledgeReportRawData)(_: String)).expects(*, *).anyNumberOfTimes() returns (Right(ResponseWrapper(internalCorrelationId, AcknowledgeReportRequest(simpleNino, simpleReportId.toString, simpleCorrelationId))))
    }
  }

}
