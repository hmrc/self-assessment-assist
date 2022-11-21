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

package uk.gov.hmrc.transactionalrisking.v1.mocks.services

import cats.data.EitherT
import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.transactionalrisking.models.auth.{AuthOutcome, RdsAuthCredentials, UserDetails}
import uk.gov.hmrc.transactionalrisking.models.domain.AssessmentReport
import uk.gov.hmrc.transactionalrisking.models.errors.MtdError
import uk.gov.hmrc.transactionalrisking.services.ServiceOutcome
import uk.gov.hmrc.transactionalrisking.services.rds.RdsAuthConnector
import scala.concurrent.ExecutionContext.Implicits.global
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

trait MockRdsAuthConnector extends MockFactory {

  val mockRdsAuthConnector: RdsAuthConnector[Future] = mock[RdsAuthConnector[Future]]

  object MockRdsAuthConnector {

    def retrieveAuthorisedBearer(): CallHandler[EitherT[Future,MtdError, RdsAuthCredentials]] = {

        (mockRdsAuthConnector.retrieveAuthorisedBearer()(_: HeaderCarrier))
          .expects(*).anyNumberOfTimes()
          .returns(EitherT.fromEither(Right(RdsAuthCredentials(UUID.randomUUID().toString,"bearer",3600))))

    }
  }

}
