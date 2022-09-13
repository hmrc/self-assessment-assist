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

package uk.gov.hmrc.transactionalrisking.services.eis

import play.api.Logger
import uk.gov.hmrc.transactionalrisking.models.domain.CalculationInfo

import java.util.UUID


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
class IntegrationFrameworkService {

  val logger: Logger = Logger("IntegrationFrameworkService")
//TODO fix me actual integration and stub pending
  def getCalculationInfo(id: UUID, nino: String): Option[CalculationInfo] = {
    logger.info(s"Attempting to get the calculation info for id [$id] and nino [$nino] ... ")
    val calculationInfo = Some(CalculationInfo(id, nino, "2021-22"))
    logger.info("... returning.")
    calculationInfo
  }

}
