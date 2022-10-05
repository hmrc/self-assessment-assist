package uk.gov.hmrc.transactionalrisking.v1.mocks.requestParsers

import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import uk.gov.hmrc.transactionalrisking.controllers.requestParsers.AcknowledgeRequestParser
import uk.gov.hmrc.transactionalrisking.models.errors.ErrorWrapper
import uk.gov.hmrc.transactionalrisking.models.request.AcknowledgeReportRawData
import uk.gov.hmrc.transactionalrisking.services.nrs.models.request.AcknowledgeReportRequest
import uk.gov.hmrc.transactionalrisking.v1.CommonTestData.{simpleCorrelationId, simpleNino, simpleReportId}

trait MockAcknowledgeRequestParser extends MockFactory {

  val mockAcknowledgeRequestParser: AcknowledgeRequestParser = mock[AcknowledgeRequestParser]

  object MockAcknowledgeRequestParser {

    def parseRequest(rawData: AcknowledgeReportRawData): CallHandler[Either[ErrorWrapper, AcknowledgeReportRequest]] = {
      (mockAcknowledgeRequestParser.parseRequest(_: AcknowledgeReportRawData)(_: String)).expects(*, *).anyNumberOfTimes() returns (Right(AcknowledgeReportRequest( simpleNino, simpleReportId.toString, simpleCorrelationId)))
    }
  }

}
