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

package uk.gov.hmrc.transactionalrisking.v1.services.cip

import play.api.Logger
import uk.gov.hmrc.transactionalrisking.v1.models.domain.{FraudDecision, FraudRiskReport, FraudRiskRequest}
import uk.gov.hmrc.transactionalrisking.v1.models.outcomes.ResponseWrapper
import uk.gov.hmrc.transactionalrisking.v1.services.ServiceOutcome

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}


@Singleton
class InsightService @Inject()() {

  val logger: Logger = Logger("InsightService")

  def assess(fraudRiskRequest: FraudRiskRequest)(implicit ec: ExecutionContext,
                                                 correlationId: String): Future[ServiceOutcome[FraudRiskReport]] = {
    logger.info(s"$correlationId::[assess] Received request for a fraud risk report ...")
    val fraudRiskReport = FraudRiskReport(1, Set.empty, Set.empty)
    logger.info(s"$correlationId::[assess] ... returning it.")
    Future(Right(ResponseWrapper(correlationId, fraudRiskReport)))
  }
}

