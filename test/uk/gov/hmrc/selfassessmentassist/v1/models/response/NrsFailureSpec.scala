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

package uk.gov.hmrc.selfassessmentassist.v1.models.response

import play.api.http.Status
import uk.gov.hmrc.selfassessmentassist.support.ConnectorSpec
import uk.gov.hmrc.selfassessmentassist.v1.models.response.nrs.NrsFailure

class NrsFailureSpec extends ConnectorSpec {
  import NrsFailure._

  "NRSFailure" should {
    "ErrorResponse should be retryable for server errors" in {
      ErrorResponse(500).retryable shouldBe true
    }

    "ErrorResponse should be retryable for timed out responses" in {
      ErrorResponse(Status.REQUEST_TIMEOUT).retryable shouldBe true
    }

    "Exception should not be retryable" in {
      Exception("some reason").retryable shouldBe false
    }

    "ExceptionThrown should not be retryable" in {
      ExceptionThrown.retryable shouldBe false
    }
  }

}
