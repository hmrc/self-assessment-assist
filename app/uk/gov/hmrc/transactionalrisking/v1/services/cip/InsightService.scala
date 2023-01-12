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

package uk.gov.hmrc.transactionalrisking.v1.services.cip

import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.transactionalrisking.utils.Logging
import uk.gov.hmrc.transactionalrisking.v1.services.ServiceOutcome
import uk.gov.hmrc.transactionalrisking.v1.services.cip.models.{FraudRiskReport, FraudRiskRequest}

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class InsightService @Inject()(connector: InsightConnector) extends Logging{

  def assess(fraudRiskRequest: FraudRiskRequest)(implicit hc: HeaderCarrier,
                                                 correlationId: String): Future[ServiceOutcome[FraudRiskReport]] = {
    logger.info(s"$correlationId::[assess] Received request for a fraud risk report ...")
    connector.assess(fraudRiskRequest)
  }
}

