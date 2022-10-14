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

package uk.gov.hmrc.transactionalrisking.v1.controllers.requestParse

import uk.gov.hmrc.transactionalrisking.controllers.requestParsers.RequestParser
import uk.gov.hmrc.transactionalrisking.controllers.requestParsers.validators.Validator
import uk.gov.hmrc.transactionalrisking.models.errors.{BadRequestError, DownstreamError, ErrorWrapper, NinoFormatError}
import uk.gov.hmrc.transactionalrisking.models.outcomes.ResponseWrapper
import uk.gov.hmrc.transactionalrisking.models.request.RawData
import uk.gov.hmrc.transactionalrisking.support.UnitSpec
import uk.gov.hmrc.transactionalrisking.v1.CommonTestData.commonTestData.{internalCorrelationIdString, simpleNino}

class RequestParserSpec extends UnitSpec {

  private val nino = simpleNino
  case class Raw(nino: String) extends RawData
  case class Request(nino: String)
  implicit val correlationId: String = internalCorrelationIdString

  trait Test {
    test =>

    val validator: Validator[Raw]

    val parser: RequestParser[Raw, Request] = new RequestParser[Raw, Request] {
      val validator: Validator[Raw] = test.validator

      protected def requestFor(data: Raw): Request = Request(data.nino)
    }
  }

  "parse" should {
    "return a Request" when {
      "the validator returns no errors" in new Test {
        lazy val validator: Validator[Raw] = (_: Raw) => Nil

        parser.parseRequest(Raw(nino)) shouldBe Right(ResponseWrapper( correlationId, Request(nino)))
      }
    }

    "return a single error" when {
      "the validator returns a single error" in new Test {
        lazy val validator: Validator[Raw] = (_: Raw) => List(NinoFormatError)

        val vl = parser.parseRequest(Raw(nino))
        val vr = Left(ErrorWrapper(correlationId, NinoFormatError, None))
        vl shouldBe vr
      }
    }

    "return multiple errors" when {
      "the validator returns multiple errors" in new Test {
        lazy val validator: Validator[Raw] = (_: Raw) => List(NinoFormatError , DownstreamError)

        val vl = parser.parseRequest(Raw(nino))
        val vr = Left(ErrorWrapper(correlationId, BadRequestError, Some(Seq(NinoFormatError, DownstreamError))))
        vl shouldBe vr
      }
    }
  }

}
