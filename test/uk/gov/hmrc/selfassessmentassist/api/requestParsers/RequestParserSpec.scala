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

package uk.gov.hmrc.selfassessmentassist.api.requestParsers

import uk.gov.hmrc.selfassessmentassist.api.TestData.CommonTestData.{correlationId, simpleNino}
import uk.gov.hmrc.selfassessmentassist.api.models.errors.{BadRequestError, ErrorWrapper, InternalError, MtdError, NinoFormatError}
import uk.gov.hmrc.selfassessmentassist.api.models.request.RawData
import uk.gov.hmrc.selfassessmentassist.api.requestParsers.validators.Validator
import uk.gov.hmrc.selfassessmentassist.support.UnitSpec

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

class RequestParserSpec extends UnitSpec {

  private val nino = simpleNino
  case class Raw(nino: String) extends RawData
  case class Request(nino: String)

  trait Test {
    test =>

    val validator: Validator[Raw]

    lazy val parser: RequestParser[Raw, Request] = new RequestParser[Raw, Request] {
      val validator: Validator[Raw] = test.validator

      protected def requestFor(data: Raw): Either[MtdError, Request] = Right(Request(data.nino))
    }

  }

  "parse" should {
    "return a Request" when {
      "the validator returns no errors" in new Test {
        val validator: Validator[Raw] = (_: Raw) => Nil

        await(parser.parseRequest(Raw(nino))(implicitly[ExecutionContext], correlationId)) shouldBe Right(Request(nino))
      }
    }

    "return a single error" when {
      "the validator returns a single error" in new Test {
        val validator: Validator[Raw] = (_: Raw) => List(NinoFormatError)

        await(parser.parseRequest(Raw(nino))(implicitly[ExecutionContext], correlationId)) shouldBe Left(
          ErrorWrapper(correlationId, NinoFormatError, None))
      }
    }

    "return multiple errors" when {
      "the validator returns multiple errors" in new Test {
        val validator: Validator[Raw] = (_: Raw) => List(NinoFormatError, InternalError)

        await(parser.parseRequest(Raw(nino))(implicitly[ExecutionContext], correlationId)) shouldBe Left(
          ErrorWrapper(correlationId, BadRequestError, Some(Seq(NinoFormatError, InternalError))))
      }
    }
  }

}
