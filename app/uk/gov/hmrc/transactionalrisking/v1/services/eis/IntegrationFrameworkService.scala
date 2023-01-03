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

package uk.gov.hmrc.transactionalrisking.v1.services.eis

import play.api.Logger
import uk.gov.hmrc.transactionalrisking.v1.models.domain.CalculationInfo
import uk.gov.hmrc.transactionalrisking.v1.models.outcomes.ResponseWrapper
import uk.gov.hmrc.transactionalrisking.v1.services.ServiceOutcome

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}


/**
 * "Integration Framework is an internal API gateway built by Enterprise Integration Services (EIS) to enable access
 * to Heads of Duty (HODs), replacing legacy systems such as DES and DAPI (Squid)."
 *
 * Source: https://confluence.tools.tax.service.gov.uk/pages/viewpage.action?pageId=245532671
 *
 * You might find it useful to also look at:
 *
 * https://confluence.tools.tax.service.gov.uk/display/DDCWLS/Integration+Framework+Stubs
 *
 */


@Singleton
class IntegrationFrameworkService @Inject()(){

  val logger: Logger = Logger("IntegrationFrameworkService")
//TODO fix me actual integration and stub pending
  def getCalculationInfo(id: UUID, nino: String)(implicit ec:ExecutionContext, correlationId: String): Future[ServiceOutcome[CalculationInfo]] = {
    logger.info(s"$correlationId::[getCalculationInfo] returning calculation info ... ")
    Future(Right( ResponseWrapper(correlationId,CalculationInfo(id, nino, "2021-22"))))
  }

}
