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

package uk.gov.hmrc.transactionalrisking.v1.services.nrs.models.request

import play.api.libs.json.{Json, OFormat, Reads, Writes}
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.auth.core.{AffinityGroup, ConfidenceLevel, CredentialRole}
import uk.gov.hmrc.transactionalrisking.utils.DateUtils

import java.time.LocalDate

case class IdentityData(internalId: Option[String] = None,
                        externalId: Option[String] = None,
                       // agentCode: Option[String] = None,//TODO revisit : this is not required as its alreay there in AgentInformation so removed
                       // credentials: Option[Credentials] = None,//TODO revist:  not present in documentation so removed
                        confidenceLevel: ConfidenceLevel,
                       // nino: Option[String] = None,//TODO revist:  not present in documentation so removed
                        dateOfBirth: Option[LocalDate] = None,
                        agentInformation: AgentInformation,
                        saUtr: Option[String] = None,
                        //name: Option[Name] = None,//TODO revist:  not present in documentation so removed
                       // email: Option[String] = None,//TODO revist:  not present in documentation so removed
                        groupIdentifier: Option[String] = None,
                        credentialRole: Option[CredentialRole],//This is coming as none
                        mdtpInformation: Option[MdtpInformation] = None,
                        itmpName: ItmpName,
                        itmpAddress: ItmpAddress,
                        itmpDateOfBirth: Option[LocalDate] = None,
                        affinityGroup: Option[AffinityGroup], // TODO does this need to be an Option?
                        credentialStrength: Option[String] = None,
                        loginTimes: LoginTimes
                       )

object IdentityData {
  implicit val localDateReads: Reads[LocalDate] = DateUtils.dateReads
  implicit val localDateWrites: Writes[LocalDate] = DateUtils.dateWrites
  implicit val credFormat: OFormat[Credentials] = Json.format[Credentials]
  implicit val nameFormat: OFormat[Name] = Json.format[Name]
  implicit val agentInfoFormat: OFormat[AgentInformation] = Json.format[AgentInformation]
  implicit val mdtpInfoFormat: OFormat[MdtpInformation] = Json.format[MdtpInformation]
  implicit val itmpNameFormat: OFormat[ItmpName] = Json.format[ItmpName]
  implicit val itmpAddressFormat: OFormat[ItmpAddress] = Json.format[ItmpAddress]
  implicit val loginTimesFormat: OFormat[LoginTimes] = Json.format[LoginTimes]
  implicit val format: OFormat[IdentityData] = Json.format[IdentityData]
}
