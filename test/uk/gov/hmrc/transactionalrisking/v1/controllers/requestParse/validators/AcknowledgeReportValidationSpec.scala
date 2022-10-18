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

package uk.gov.hmrc.transactionalrisking.v1.controllers.requestParse.validators

import uk.gov.hmrc.transactionalrisking.models.errors.{FormatReportIdError, NinoFormatError}
import uk.gov.hmrc.transactionalrisking.models.request.AcknowledgeReportRawData
import uk.gov.hmrc.transactionalrisking.support.UnitSpec
import uk.gov.hmrc.transactionalrisking.v1.CommonTestData.commonTestData.{simpleNino, simpleNinoInvalid, simpleRDSCorrelationID, simpleReportID, simpleReportaIDStrangeCharsString}
import uk.gov.hmrc.transactionalrisking.controllers.requestParsers.validators.AcknowledgeReportValidator

class AcknowledgeReportValidationSpec extends UnitSpec {
  val validator:AcknowledgeReportValidator = new AcknowledgeReportValidator

  "running a validation" should {
    "return no errors" when {
      "a valid request" in {
        val acknowledgeReportRawData:AcknowledgeReportRawData=AcknowledgeReportRawData(simpleNino, simpleReportID.toString, simpleRDSCorrelationID)
        val vl = validator.validate(acknowledgeReportRawData)
        val vr = Nil

        vl shouldBe vr
      }


      "an invalid nino." in {

        val acknowledgeReportRawData:AcknowledgeReportRawData=AcknowledgeReportRawData(simpleNinoInvalid, simpleReportID.toString, simpleRDSCorrelationID)

        val vl = validator.validate(acknowledgeReportRawData)
        val vr = Seq(NinoFormatError)

        vl shouldBe vr
      }

      "an invalid reportId." in {

        val acknowledgeReportRawData: AcknowledgeReportRawData = AcknowledgeReportRawData(simpleNino, simpleReportaIDStrangeCharsString, simpleRDSCorrelationID)

        val vl = validator.validate(acknowledgeReportRawData)
        val vr = Seq(FormatReportIdError)

        vl shouldBe vr
      }

//TODO check syntax correlation Id.
//      "an invalid rdsCorrelationId." in {
//
//        val acknowledgeReportRawData: AcknowledgeReportRawData = AcknowledgeReportRawData(simpleNinoInvalid, simpleReportId.toString, simpleRDSCorrelationId)
//
//        val vl = validator.validate(acknowledgeReportRawData)
//        val vr = Seq(NinoFormatError)
//
//        vl shouldBe vr
//      }


      "all invalid nino, reportId, correlationId(is ignored)." in {

        val acknowledgeReportRawData: AcknowledgeReportRawData = AcknowledgeReportRawData(simpleNinoInvalid, simpleReportaIDStrangeCharsString, simpleRDSCorrelationID)

        val vl = validator.validate(acknowledgeReportRawData)
        val vr = Seq(NinoFormatError, FormatReportIdError)

        vl shouldBe vr
      }

    }
  }
}
