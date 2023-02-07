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

package uk.gov.hmrc.transactionalrisking.v1.services.ifs

import play.api.libs.json.{JsObject, JsValue, Json}
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.auth.core.{ConfidenceLevel, User}
import uk.gov.hmrc.transactionalrisking.v1.TestData.CommonTestData
import uk.gov.hmrc.transactionalrisking.v1.TestData.CommonTestData._
import uk.gov.hmrc.transactionalrisking.v1.services.ifs.models.request.{IFRequest, Messages}
import uk.gov.hmrc.transactionalrisking.v1.services.nrs.models.request.{IdentityData, Metadata, SearchKeys}
import uk.gov.hmrc.transactionalrisking.v1.services.rds.RdsTestData.assessmentRequestForSelfAssessment

import java.time.{Instant, LocalDate, OffsetDateTime}

object IdentityDataTestData {

  val correctJson: JsValue = Json.parse(
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

  def correctModel: IdentityData = IdentityData(
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
}

object MetadataTestData {
  val correctJson: JsValue = Json.parse(
    s"""
       |{
       |    "businessId": "saa",
       |    "notableEvent": "saa-report-generated",
       |    "payloadContentType": "application/json",
       |    "payloadSha256Checksum":"2c98a3e52aed1f06728e35e4f47699bd4af6f328c3dabfde998007382dba86ce",
       |    "userSubmissionTimestamp": "2018-04-07T12:13:25Z",
       |    "identityData": ${IdentityDataTestData.correctJson},
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

  val correctModel: Metadata = Metadata(
    businessId = "saa",
    notableEvent = "saa-report-generated",
    payloadContentType = "application/json",
    payloadSha256Checksum = "2c98a3e52aed1f06728e35e4f47699bd4af6f328c3dabfde998007382dba86ce",
    userSubmissionTimestamp = "2018-04-07T12:13:25Z",
    identityData = Some(IdentityDataTestData.correctModel),
    userAuthToken = "Bearer AbCdEf123456...",
    headerData = Json.toJson(Map(
      "Gov-Client-Public-IP" -> "127.0.0.0",
      "Gov-Client-Public-Port" -> "12345",
      "Gov-Client-Device-ID" -> "beec798b-b366-47fa-b1f8-92cede14a1ce",
      "Gov-Client-User-ID" -> "alice_desktop",
      "Gov-Client-Timezone" -> "GMT+3",
      "Gov-Client-Local-IP" -> "10.1.2.3",
      "Gov-Client-Screen-Resolution" -> "1920x1080",
      "Gov-Client-Window-Size" -> "1256x803",
      "Gov-Client-Colour-Depth" -> "24"
    )),
    searchKeys =
      SearchKeys(
        reportId = simpleReportId.toString
      )
  )
}

object FullRequestTestData {
  val correctJson: JsObject = Json.obj(
    "payload" -> "XXX-base64checksum-XXX",
    "metadata" -> MetadataTestData.correctJson
  )


  val correctModel: IFRequest =
  IFRequest(
    serviceRegime = "self-assessment-assist",
    eventName = "GenerateReport",
    eventTimestamp = OffsetDateTime.now(),
    feedbackId = CommonTestData.simpleAcknowledgeNewRdsAssessmentReport.feedbackId.get.toString,
    metadata = List(
      Map("nino" -> "nino"),
      Map("taxYear" -> "2023-2024"),
      Map("calculationId" -> "calculationId"),
      Map("customerType" -> assessmentRequestForSelfAssessment.customerType.toString),
      Map("agentReferenceNumber" -> assessmentRequestForSelfAssessment.agentRef.getOrElse("")),
      Map("calculationTimestamp" -> "timestamp")
    ),
    payload = Some(Messages(Some(Vector())))
  )

  val correctJsonString: String = IFRequest.formats.writes(correctModel).toString()
}

object NrsResponseTestData {

  val correctJson: JsValue = Json.parse(
    """
      |{
      |  "nrSubmissionId": "anId",
      |  "cadesTSignature": "aSignature",
      |  "timestamp": "aTimeStamp"
      |}
    """.stripMargin
  )
}