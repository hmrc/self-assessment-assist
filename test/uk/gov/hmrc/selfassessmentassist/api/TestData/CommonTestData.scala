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

package uk.gov.hmrc.selfassessmentassist.api.TestData

import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.auth.core.{AffinityGroup, ConfidenceLevel, User}
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.auth.core.retrieve.{AgentInformation, Credentials, ItmpAddress, ItmpName, LoginTimes, MdtpInformation, Name}
import uk.gov.hmrc.selfassessmentassist.api.models.auth.UserDetails
import uk.gov.hmrc.selfassessmentassist.api.models.domain.PreferredLanguage.PreferredLanguage
import uk.gov.hmrc.selfassessmentassist.api.models.domain._
import uk.gov.hmrc.selfassessmentassist.utils.DateUtils
import uk.gov.hmrc.selfassessmentassist.v1.models.domain._
import uk.gov.hmrc.selfassessmentassist.v1.models.request.GenerateReportRawData
import uk.gov.hmrc.selfassessmentassist.v1.models.request.cip.{FraudRiskReport, FraudRiskRequest}
import uk.gov.hmrc.selfassessmentassist.v1.models.request.nrs.{AcknowledgeReportRequest, IdentityData, Metadata, SearchKeys}
import uk.gov.hmrc.selfassessmentassist.v1.models.response.nrs.NrsResponse
import uk.gov.hmrc.selfassessmentassist.v1.models.response.rds.RdsAssessmentReport
import uk.gov.hmrc.selfassessmentassist.v1.utils.StubResource.{loadAckResponseTemplate, loadSubmitResponseTemplate}

import java.time.{Instant, LocalDate, LocalDateTime, Month, OffsetDateTime, ZoneOffset}
import java.util.UUID

object CommonTestData {

  val simpleNino: String                = "AA000000B"
  val simpleCalculationId: UUID         = UUID.fromString("f2fb30e5-4ab6-4a29-b3c1-c00000000001")
  val calculationIdWithNoFeedback: UUID = UUID.fromString("201204b4-06e3-4fef-a555-6fd0877dc7ca")
  val noCalculationFound: UUID          = UUID.fromString("201404b4-06e3-4fef-a555-6fd0877dc7ca")
  val simpleRDSCorrelationId: String    = "5fht738957jfjf845jgjf855"
  val simpleCIPCorrelationId: String    = "5fht738957jfjf845jgjf855"
  val correlationId: String             = "f2fb30e5-4ab6-4a29-b3c1-c00000011111"
  val simpleReportId: UUID              = UUID.fromString(correlationId)

  val simpleRiskTitle     = "title"
  val simpleRiskBody      = "body"
  val simpleRiskAction    = "action"
  val simpleLinkTitle     = "title"
  val simpleLinkUrl       = "url"
  val simplePath          = "path"
  val simpleTaxYearEndInt = 2022
  val simpleTaxYear       = "2021-22"

  val simpleExternalOrigin: Origin = External
  val simpleInternalOrigin: Origin = Internal

  val simpleIndividualUserDetails: UserDetails = UserDetails(
    userType = AffinityGroup.Individual,
    agentReferenceNumber = None,
    clientID = "clientId",
    identityData = None
  )

  val simpleCustomerType: CustomerType.Value     = CustomerType.TaxPayer
  val simplePreferredLanguage: PreferredLanguage = PreferredLanguage.English
  val simpleAgentRef: Option[Nothing]            = None

  val simpleAssessmentRequestForSelfAssessment: AssessmentRequestForSelfAssessment = AssessmentRequestForSelfAssessment(
    calculationId = simpleCalculationId,
    nino = simpleNino,
    preferredLanguage = PreferredLanguage.English,
    customerType = CustomerType.TaxPayer,
    agentRef = None,
    taxYear = DesTaxYear.fromMtd(simpleTaxYear).toString
  )

  val simpleAssessmentReport: AssessmentReport = AssessmentReport(
    reportId = simpleReportId,
    risks = Seq(
      Risk(
        title = simpleRiskTitle,
        body = simpleRiskBody,
        action = simpleRiskAction,
        links = Seq(Link(simpleLinkTitle, simpleLinkUrl)),
        path = simplePath)),
    nino = simpleNino,
    taxYear = DesTaxYear.fromMtd(simpleTaxYear).toString,
    calculationId = simpleCalculationId,
    rdsCorrelationId = simpleRDSCorrelationId
  )

  val simpleCalculationTimestamp: LocalDateTime = LocalDateTime.parse("2019-02-15T09:35:15.094Z", DateUtils.dateTimePattern)

  val simpleGenerateReportRawData: GenerateReportRawData =
    GenerateReportRawData(simpleCalculationId.toString, simpleNino, PreferredLanguage.English, CustomerType.TaxPayer, None, simpleTaxYear)

  val simpleAssessmentReportMtdJson: JsValue = Json.toJson[AssessmentReport](simpleAssessmentReport)

  val simpleFraudRiskRequest: FraudRiskRequest =
    new FraudRiskRequest(nino = Some(simpleNino), taxYear = Some(simpleTaxYear), fraudRiskHeaders = Map.empty[String, String])

  val simpleFraudRiskReport: FraudRiskReport = new FraudRiskReport(0, simpleCIPCorrelationId, Seq.empty)

  val simpleGenerateReportControllerNrsID: String = "537490b4-06e3-4fef-a555-6fd0877dc7ca"
  val simpleSubmissionTimestamp: OffsetDateTime   = OffsetDateTime.of(2022, Month.JANUARY.getValue, 1, 12, 0, 0, 0, ZoneOffset.UTC)

  val reportSubmissionId: String                     = UUID.fromString("f2fb30e5-4ab6-4a29-b3c1-c0000000010").toString
  val simpleNRSResponseReportSubmission: NrsResponse = NrsResponse(reportSubmissionId)

  val acknowledgeSubmissionId: String                     = UUID.fromString("f2fb30e5-4ab6-4a29-b3c1-c0000000011").toString
  val simpleNRSResponseAcknowledgeSubmission: NrsResponse = NrsResponse(acknowledgeSubmissionId)

  val simpleAcknowledgeReportRequest: AcknowledgeReportRequest = AcknowledgeReportRequest(simpleNino, simpleReportId.toString, simpleRDSCorrelationId)

  val rdsSubmissionReportJson: JsValue = loadSubmitResponseTemplate(simpleCalculationId.toString, simpleReportId.toString, simpleRDSCorrelationId)
  val rdsNewSubmissionReport: RdsAssessmentReport = rdsSubmissionReportJson.as[RdsAssessmentReport]

  val simpleAssessmentReportWrapper: AssessmentReportWrapper =
    AssessmentReportWrapper(simpleCalculationTimestamp, simpleAssessmentReport, rdsNewSubmissionReport)

  val rdsAssessmentAckJson: JsValue = loadAckResponseTemplate(simpleReportId.toString, nino = simpleNino, responseCode = 202)
  val simpleAcknowledgeNewRdsAssessmentReport: RdsAssessmentReport = rdsAssessmentAckJson.as[RdsAssessmentReport]

  val invalidUUID: UUID         = new UUID(0, 1)
  val invalidUUIDString: String = invalidUUID.toString

  val simpleCalculationIdStrangeCharsString: String = "f2fb30e5#4ab6#4a29-b3c1-c00000000001"
  val simpleReportIdStrangeCharsString: String      = "f2fb30e5#4ab6#4a29-b3c1-c00000000001"

  val simpleNinoInvalid: String     = "AA000000Z"
  val simpleTaxYearInvalid1: String = "2020-25"
  val simpleTaxYearInvalid2: String = "200-1"
  val simpleTaxYearInvalid3: String = "sjdhakjd"

  val identityCorrectJson: JsValue = Json.parse(
    """{
      |  "internalId": "some-id",
      |  "externalId": "some-id",
      |  "agentCode": "TZRXXV",
      |  "credentials": {"providerId": "12345-credId",
      |  "providerType": "GovernmentGateway"},
      |  "confidenceLevel": 200,
      |  "nino": "DH00475D",
      |  "saUtr": "Utr",
      |  "name": { "name": "test", "lastName": "test" },
      |  "dateOfBirth": "1985-01-01",
      |  "email":"test@test.com",
      |  "agentInformation": {
      |    "agentId": "BDGL",
      |    "agentCode" : "TZRXXV",
      |    "agentFriendlyName" : "Bodgitt & Legget LLP"
      |  },
      |  "groupIdentifier" : "GroupId",
      |  "credentialRole": "User",
      |  "mdtpInformation" : {"deviceId" : "DeviceId",
      |    "sessionId": "SessionId" },
      |  "itmpName" : {},
      |  "itmpAddress" : {},
      |  "affinityGroup": "Agent",
      |  "credentialStrength": "strong",
      |  "loginTimes": {
      |    "currentLogin": "2016-11-27T09:00:00Z",
      |    "previousLogin": "2016-11-01T12:00:00Z"
      |  }
      |}
      """.stripMargin
  )

  def identityCorrectModel: IdentityData = IdentityData(
    internalId = Some("some-id"),
    externalId = Some("some-id"),
    agentCode = Some("TZRXXV"),
    credentials = Some(Credentials("12345-credId", "GovernmentGateway")),
    confidenceLevel = ConfidenceLevel.L200,
    nino = Some("DH00475D"),
    saUtr = Some("Utr"),
    name = Some(Name(Some("test"), Some("test"))),
    dateOfBirth = Some(LocalDate.parse("1985-01-01")),
    email = Some("test@test.com"),
    agentInformation = AgentInformation(agentCode = Some("TZRXXV"), agentFriendlyName = Some("Bodgitt & Legget LLP"), agentId = Some("BDGL")),
    groupIdentifier = Some("GroupId"),
    credentialRole = Some(User),
    mdtpInformation = Some(MdtpInformation("DeviceId", "SessionId")),
    itmpName = ItmpName(None, None, None),
    itmpDateOfBirth = None,
    itmpAddress = ItmpAddress(None, None, None, None, None, None, None, None),
    affinityGroup = Some(Agent),
    credentialStrength = Some("strong"),
    loginTimes = LoginTimes(
      Instant.parse("2016-11-27T09:00:00Z"),
      Some(Instant.parse("2016-11-01T12:00:00Z"))
    )
  )

  val metaDataCorrectJson: JsValue = Json.parse(
    s"""
       |{
       |    "businessId": "saa",
       |    "notableEvent": "saa-report-generated",
       |    "payloadContentType": "application/json",
       |    "payloadSha256Checksum":"2c98a3e52aed1f06728e35e4f47699bd4af6f328c3dabfde998007382dba86ce",
       |    "userSubmissionTimestamp": "2018-04-07T12:13:25Z",
       |    "identityData": $identityCorrectJson,
       |    "userAuthToken": "Bearer AbCdEf123456...",
       |    "headerData": {
       |      "Gov-Client-Window-Size": "1256x803",
       |      "Gov-Client-Local-IP": "10.1.2.3",
       |      "Gov-Client-Device-ID": "beec798b-b366-47fa-b1f8-92cede14a1ce",
       |      "Gov-Client-Screen-Resolution": "1920x1080",
       |      "Gov-Client-Public-Port": "12345",
       |      "Gov-Client-Public-IP": "127.0.0.0",
       |      "Gov-Client-User-ID": "alice_desktop",
       |      "Gov-Client-Timezone": "GMT+3",
       |      "Gov-Client-Colour-Depth": "24"
       |    },
       |    "searchKeys": {
       |      "reportId": "${simpleReportId.toString}"
       |    }
       |}
      """.stripMargin
  )

  val metaDataCorrectModel: Metadata = Metadata(
    businessId = "saa",
    notableEvent = "saa-report-generated",
    payloadContentType = "application/json",
    payloadSha256Checksum = "2c98a3e52aed1f06728e35e4f47699bd4af6f328c3dabfde998007382dba86ce",
    userSubmissionTimestamp = "2018-04-07T12:13:25Z",
    identityData = Some(identityCorrectModel),
    userAuthToken = "Bearer AbCdEf123456...",
    headerData = Json.toJson(
      Map(
        "Gov-Client-Public-IP"         -> "127.0.0.0",
        "Gov-Client-Public-Port"       -> "12345",
        "Gov-Client-Device-ID"         -> "beec798b-b366-47fa-b1f8-92cede14a1ce",
        "Gov-Client-User-ID"           -> "alice_desktop",
        "Gov-Client-Timezone"          -> "GMT+3",
        "Gov-Client-Local-IP"          -> "10.1.2.3",
        "Gov-Client-Screen-Resolution" -> "1920x1080",
        "Gov-Client-Window-Size"       -> "1256x803",
        "Gov-Client-Colour-Depth"      -> "24"
      )),
    searchKeys = SearchKeys(
      reportId = simpleReportId.toString
    )
  )

  val nrsResponseJson: JsValue = Json.parse(
    """
      |{
      |  "nrSubmissionId": "anId",
      |  "cadesTSignature": "aSignature",
      |  "timestamp": "aTimeStamp"
      |}
    """.stripMargin
  )
}
