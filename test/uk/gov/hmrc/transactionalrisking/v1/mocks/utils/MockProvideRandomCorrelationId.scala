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

package uk.gov.hmrc.transactionalrisking.v1.mocks.utils

import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import uk.gov.hmrc.transactionalrisking.utils.ProvideRandomCorrelationId
import uk.gov.hmrc.transactionalrisking.v1.CommonTestData.commonTestData._

trait MockProvideRandomCorrelationId extends MockFactory {
  val mockProvideRandomCorrelationId = mock[ProvideRandomCorrelationId ]

  object MockProvideRandomCorrelationId {

    def getRandomCorrelationId(): CallHandler[String] = {
      (mockProvideRandomCorrelationId.getRandomCorrelationId _)
        .expects
        .returns( internalCorrelationIdString )
    }
  }
}
