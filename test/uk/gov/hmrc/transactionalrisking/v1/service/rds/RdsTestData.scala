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

package uk.gov.hmrc.transactionalrisking.v1.service.rds

import play.api.libs.json._
import uk.gov.hmrc.transactionalrisking.models.domain.{AssessmentReport, AssessmentRequestForSelfAssessment, CustomerType, DesTaxYear, FraudDecision, FraudRiskHeader, FraudRiskReport, Link, PreferredLanguage, Risk}
import uk.gov.hmrc.transactionalrisking.models.errors.{ErrorWrapper, MtdError}
import uk.gov.hmrc.transactionalrisking.models.outcomes.ResponseWrapper
import uk.gov.hmrc.transactionalrisking.services.ServiceOutcome
import uk.gov.hmrc.transactionalrisking.services.rds.models.request.RdsRequest
import uk.gov.hmrc.transactionalrisking.services.rds.models.request.RdsRequest.{DataWrapper, MetadataWrapper}
import uk.gov.hmrc.transactionalrisking.services.rds.models.response.NewRdsAssessmentReport
import uk.gov.hmrc.transactionalrisking.v1.CommonTestData
import uk.gov.hmrc.transactionalriskingsimulator.domain.WatchlistFlag
import uk.gov.hmrc.transactionalrisking.v1.CommonTestData.{simpeTaxYear, simpleReportId}

import java.util.UUID

trait RdsTestData {

  val rdsRequest: ServiceOutcome[RdsRequest] = Right(
    ResponseWrapper(CommonTestData.correlationId,
      RdsRequest(
        Seq()
      )
    )
  )

  val acknowledgeReportRequest: RdsRequest = RdsRequest(Seq())

  val rdsRequestError: ServiceOutcome[RdsRequest] = Left(ErrorWrapper(CommonTestData.correlationId,MtdError(code = "", message = "")))

  var rdsRequestBody: String = """
                                 |{
                                 |  "inputs": [
                                 |    {
                                 |      "name": "calculationId",
                                 |      "value": "537490b4-06e3-4fef-a555-6fd0877dc7ca"
                                 |    },
                                 |    {
                                 |      "name": "nino",
                                 |      "value": "QQ123456A"
                                 |    },
                                 |    {
                                 |      "name": "taxYear",
                                 |      "value": 2022
                                 |    },
                                 |    {
                                 |      "name": "customerType",
                                 |      "value": "2022"
                                 |    },
                                 |    {
                                 |      "name": "agentRef",
                                 |      "value": null
                                 |    },
                                 |    {
                                 |      "name": "preferredLanguage",
                                 |      "value": "EN"
                                 |    },
                                 |    {
                                 |      "name": "fraudRiskReportDecision",
                                 |      "value": "A"
                                 |    },
                                 |    {
                                 |      "name": "fraudRiskReportScore",
                                 |      "value": 2
                                 |    },
                                 |    {
                                 |      "name": "fraudRiskReportHeaders",
                                 |      "value": [
                                 |        {
                                 |          "metadata": [
                                 |            {
                                 |              "KEY": "string"
                                 |            },
                                 |            {
                                 |              "VALUE": "string"
                                 |            }
                                 |          ]
                                 |        },
                                 |        {
                                 |          "data": [
                                 |            [
                                 |              "key_one",
                                 |              "value_one"
                                 |            ],
                                 |            [
                                 |              "key_two",
                                 |              "value_two"
                                 |            ]
                                 |          ]
                                 |        }
                                 |      ]
                                 |    },
                                 |    {
                                 |      "name": "fraudRiskReportWatchlistFlags",
                                 |      "value": [
                                 |        {
                                 |          "metadata": [
                                 |            {
                                 |              "NAME": "string"
                                 |            }
                                 |          ]
                                 |        },
                                 |        {
                                 |          "data": [
                                 |            [
                                 |              "name1"
                                 |            ],
                                 |            [
                                 |              "name2"
                                 |            ],
                                 |            [
                                 |              "name3"
                                 |            ]
                                 |          ]
                                 |        }
                                 |      ]
                                 |    }
                                 |  ]
                                 |}'""".stripMargin

  val rdsSubmissionResponse =
    s"""
       |{
       |    "links": [],
       |    "version": 2,
       |    "moduleId": "HMRC_ASSIST_ITSA_FINSUB_FEEDBACK",
       |    "stepId": "execute",
       |    "executionState": "completed",
       |    "outputs": [
       |        {
       |            "name": "welshActions",
       |            "value": [
       |                {
       |                    "metadata": [
       |                        {
       |                            "TITLE": "string"
       |                        },
       |                        {
       |                            "MESSAGE": "string"
       |                        },
       |                        {
       |                            "ACTION": "string"
       |                        },
       |                        {
       |                            "LINKTITLE": "string"
       |                        },
       |                        {
       |                            "LINKURL": "string"
       |                        },
       |                        {
       |                            "PATH": "string"
       |                        }
       |                    ]
       |                },
       |                {
       |                    "data": [
       |                        [
       |                            "Ffynhonnell Incwm Di-Fusnes",
       |                            "Rydych wedi datgan benthyciad teulu fel ffynhonnell eich incwm. Bu newidiadau i'r rheolau ynghylch ffynonellau nad ydynt yn ymwneud â busnes y gallwch eu datgan, darllenwch y canllawiau priodol i weld sut mae hyn yn effeithio arnoch chi.",
       |                            "Gwirio Canllawiau",
       |                            "[Canllawiau ITSA, Arweiniad i Ffynonellau Incwm]",
       |                            "[www.itsa/cym.gov.uk, www.itsa/incomesources.gov.uk]",
       |                            "general/non_business_income_sources/income_source"
       |                        ],
       |                        [
       |                            "Trosiant",
       |                            "Mae'n ymddangos bod eich trosiant datganedig o £80,000 yn is na'r disgwyl yn seiliedig ar eich ffynonellau incwm, cadarnhewch y cyfrifir am yr holl drosiant cyn cyflwyno.",
       |                            "Gwiriwch y trosiant",
       |                            "[Cyfrifo am Incwm]",
       |                            "[www.itsa/incomecompliance.gov.uk]",
       |                            "general/total_declared_turnover"
       |                        ]
       |                    ]
       |                }
       |            ]
       |        },
       |        {
       |            "name": "englishActions",
       |            "value": [
       |                {
       |                    "metadata": [
       |                        {
       |                            "TITLE": "string"
       |                        },
       |                        {
       |                            "MESSAGE": "string"
       |                        },
       |                        {
       |                            "ACTION": "string"
       |                        },
       |                        {
       |                            "LINKTITLE": "string"
       |                        },
       |                        {
       |                            "LINKURL": "string"
       |                        },
       |                        {
       |                            "PATH": "string"
       |                        }
       |                    ]
       |                },
       |                {
       |                    "data": [
       |                        [
       |                            "Non-Business Income Source",
       |                            "You have declared family loan as a source of your income. There have been changes to the rules around non-business sources you may declare, please check the appropriate guidance to see how this impacts you.",
       |                            "Check guidance",
       |                            "[ITSA Guidance, Income Source Guidance]",
       |                            "[www.itsa.gov.uk, www.itsa/incomesources.gov.uk]",
       |                            "general/non_business_income_sources/income_source"
       |                        ],
       |                        [
       |                            "Turnover",
       |                            "Your declared turnover of £80,000 appears to be lower than expected based on your income sources, please confirm all turnover is accounted for before submission.",
       |                            "Check turnover",
       |                            "[Accounting for Income]",
       |                            "[www.itsa/incomecompliance.gov.uk]",
       |                            "general/total_declared_turnover"
       |                        ]
       |                    ]
       |                }
       |            ]
       |        },
       |        {
       |      "identifiers": [
       |        {
       |            "name": "feedbackID",
       |            "value": "a365c0b4-06e3-4fef-a555-6fd0877dc7c"
       |        },
       |        {
       |            "name": "calculationID",
       |            "value": "537490b4-06e3-4fef-a555-6fd0877dc7ca"
       |        },
       |        {
       |            "name": "correlationID",
       |            "value": "5fht738957jfjf845jgjf855"
       |        }
       |        ]
       |       }
       |    ]
       |}
       |
       |""".stripMargin

  private val rdsAssessmentReportJson = Json.parse(rdsSubmissionResponse)
  val rdsAssessmentReport: NewRdsAssessmentReport = rdsAssessmentReportJson.as[NewRdsAssessmentReport]

  val assessmentRequestForSelfAssessment = AssessmentRequestForSelfAssessment(
    calculationId = UUID.fromString("a365c0b4-06e3-4fef-a555-06fd0877dc7c"),
    nino = "AA00000B",
    preferredLanguage = PreferredLanguage.English,
    customerType = CustomerType.TaxPayer,
    agentRef = None,
    taxYear = "2022"
  )

  val fraudRiskReport = FraudRiskReport(
    decision = FraudDecision.Accept,
    score = 10,
    headers = Set(FraudRiskHeader("key", "value")),
    watchlistFlags = Set(WatchlistFlag("flag"))
  )

  val requestSO: ServiceOutcome[RdsRequest] = Right(
    ResponseWrapper(CommonTestData.correlationId,
      RdsRequest(
        Seq(
          RdsRequest.InputWithString("calculationId", assessmentRequestForSelfAssessment.calculationId.toString),
          RdsRequest.InputWithString("nino", assessmentRequestForSelfAssessment.nino),
          RdsRequest.InputWithString("taxYear", assessmentRequestForSelfAssessment.taxYear),
          RdsRequest.InputWithString("customerType", assessmentRequestForSelfAssessment.customerType.toString),
          RdsRequest.InputWithString("agentRef", assessmentRequestForSelfAssessment.agentRef.getOrElse("")),
          RdsRequest.InputWithString("preferredLanguage", assessmentRequestForSelfAssessment.preferredLanguage.toString),
          RdsRequest.InputWithString("fraudRiskReportDecision", fraudRiskReport.decision.toString),
          RdsRequest.InputWithInt("fraudRiskReportScore", fraudRiskReport.score),
          RdsRequest.InputWithObject("fraudRiskReportHeaders",
            Seq(
              MetadataWrapper(
                Seq(
                  Map("KEY" -> "string"),
                  Map("VALUE" -> "string")
                )),
              DataWrapper(fraudRiskReport.headers.map(header => Seq(header.key, header.value)).toSeq)
            )
          ),
          RdsRequest.InputWithObject("fraudRiskReportWatchlistFlags",
            Seq(
              MetadataWrapper(
                Seq(
                  Map("NAME" -> "string")
                )),
              DataWrapper(fraudRiskReport.watchlistFlags.map(flag => Seq(flag.name)).toSeq)
            )
          )
        )
      )
    )
  )

  val risks = Seq(
    Risk(
      "Non-Business Income Source",
      "You have declared family loan as a source of your income. There have been changes to the rules around non-business sources you may declare, please check the appropriate guidance to see how this impacts you.",
      "Check guidance",
      List(Link("[ITSA Guidance, Income Source Guidance]", "[www.itsa.gov.uk, www.itsa/incomesources.gov.uk]")),
      "general/non_business_income_sources/income_source"
    ),
    Risk(
      "Turnover",
      "Your declared turnover of £80,000 appears to be lower than expected based on your income sources, please confirm all turnover is accounted for before submission.",
      "Check turnover",
      List(Link("[Accounting for Income]","[www.itsa/incomecompliance.gov.uk]")),
      "general/total_declared_turnover"
    )
  )

  val assessmentReport = AssessmentReport(
    reportId = assessmentRequestForSelfAssessment.calculationId,
    risks = risks,
    nino = assessmentRequestForSelfAssessment.nino,
    taxYear = DesTaxYear.fromDesIntToString(assessmentRequestForSelfAssessment.taxYear.toInt) ,
    calculationId = assessmentRequestForSelfAssessment.calculationId,
    correlationID = "5fht738957jfjf845jgjf855"
  )
}
