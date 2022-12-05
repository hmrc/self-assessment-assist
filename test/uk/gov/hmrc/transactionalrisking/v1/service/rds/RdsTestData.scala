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

import uk.gov.hmrc.transactionalrisking.v1.TestData.CommonTestData.commonTestData._
import uk.gov.hmrc.transactionalrisking.v1.models.domain._
import uk.gov.hmrc.transactionalrisking.v1.models.errors.{ErrorWrapper, MtdError}
import uk.gov.hmrc.transactionalrisking.v1.services.ServiceOutcome
import uk.gov.hmrc.transactionalrisking.v1.services.rds.models.request.RdsRequest
import uk.gov.hmrc.transactionalrisking.v1.services.rds.models.request.RdsRequest.{DataWrapper, MetadataWrapper}


object RdsTestData {

  val acknowledgeReportRequest: RdsRequest = RdsRequest(Seq( ))

  val rdsRequestError: ServiceOutcome[RdsRequest] = Left(ErrorWrapper(internalCorrelationIDImplicit,MtdError(code = "", message = "")))

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

  val rdsSubmissionResponse = s"""{
                                    |  "links": [],
                                    |  "version": 2,
                                    |  "moduleId": "HMRC_ASSIST_ITSA_FINSUB_FEEDBACK",
                                    |  "stepId": "execute",
                                    |  "executionState": "completed",
                                    |  "metadata": {
                                    |    "module_id": "HMRC_ASSIST_ITSA_FINSUB_FEEDBACK",
                                    |    "step_id": "execute"
                                    |  },
                                    |  "outputs": [
                                    |    {
                                    |      "name": "welshActions",
                                    |      "value": [
                                    |        {
                                    |          "metadata": [
                                    |            {
                                    |              "TITLE": "string"
                                    |            },
                                    |            {
                                    |              "MESSAGE": "string"
                                    |            },
                                    |            {
                                    |              "ACTION": "string"
                                    |            },
                                    |            {
                                    |              "LINKTITLE": "string"
                                    |            },
                                    |            {
                                    |              "LINKURL": "string"
                                    |            },
                                    |            {
                                    |              "PATH": "string"
                                    |            }
                                    |          ]
                                    |        },
                                    |        {
                                    |          "data": [
                                    |            [
                                    |              "Ffynhonnell Incwm Di-Fusnes",
                                    |              "Rydych wedi datgan benthyciad teulu fel ffynhonnell eich incwm. Bu newidiadau i'r rheolau ynghylch ffynonellau nad ydynt yn ymwneud â busnes y gallwch eu datgan, darllenwch y canllawiau priodol i weld sut mae hyn yn effeithio arnoch chi.",
                                    |              "Gwirio Canllawiau",
                                    |              "[Canllawiau ITSA, Arweiniad i Ffynonellau Incwm]",
                                    |              "[www.itsa/cym.gov.uk, www.itsa/incomesources.gov.uk]",
                                    |              "general/non_business_income_sources/income_source"
                                    |            ],
                                    |            [
                                    |              "Trosiant",
                                    |              "Mae'n ymddangos bod eich trosiant datganedig o £80,000 yn is na'r disgwyl yn seiliedig ar eich ffynonellau incwm, cadarnhewch y cyfrifir am yr holl drosiant cyn cyflwyno.",
                                    |              "Gwiriwch y trosiant",
                                    |              "[Cyfrifo am Incwm]",
                                    |              "[www.itsa/incomecompliance.gov.uk]",
                                    |              "general/total_declared_turnover"
                                    |            ]
                                    |          ]
                                    |        }
                                    |      ]
                                    |    },
                                    |    {
                                    |      "name": "englishActions",
                                    |      "value": [
                                    |        {
                                    |          "metadata": [
                                    |            {
                                    |              "TITLE": "string"
                                    |            },
                                    |            {
                                    |              "MESSAGE": "string"
                                    |            },
                                    |            {
                                    |              "ACTION": "string"
                                    |            },
                                    |            {
                                    |              "LINKTITLE": "string"
                                    |            },
                                    |            {
                                    |              "LINKURL": "string"
                                    |            },
                                    |            {
                                    |              "PATH": "string"
                                    |            }
                                    |          ]
                                    |        },
                                    |        {
                                    |          "data": [
                                    |            [
                                    |              "Non-Business Income Source",
                                    |              "You have declared family loan as a source of your income. There have been changes to the rules around non-business sources you may declare, please check the appropriate guidance to see how this impacts you.",
                                    |              "Check guidance",
                                    |              "[ITSA Guidance, Income Source Guidance]",
                                    |              "[www.itsa.gov.uk, www.itsa/incomesources.gov.uk]",
                                    |              "general/non_business_income_sources/income_source"
                                    |            ],
                                    |            [
                                    |              "Turnover",
                                    |              "Your declared turnover of £80,000 appears to be lower than expected based on your income sources, please confirm all turnover is accounted for before submission.",
                                    |              "Check turnover",
                                    |              "[Accounting for Income]",
                                    |              "[www.itsa/incomecompliance.gov.uk]",
                                    |              "general/total_declared_turnover"
                                    |            ]
                                    |          ]
                                    |        }
                                    |      ]
                                    |    },
                                    |    {
                                    |      "name": "typeIDs",
                                    |      "value": [
                                    |        {
                                    |          "metadata": [
                                    |            {
                                    |              "typeID": "string"
                                    |            }
                                    |          ]
                                    |        },
                                    |        {
                                    |          "data": [
                                    |            [
                                    |              "001"
                                    |            ],
                                    |            [
                                    |              "002"
                                    |            ]
                                    |          ]
                                    |        }
                                    |      ]
                                    |    },
                                    |    {
                                    |      "name": "feedbackId",
                                    |      "value": "a365c0b4-06e3-4fef-a555-6fd0877dc7c"
                                    |    },
                                    |    {
                                    |      "name": "calculationId",
                                    |      "value": "537490b4-06e3-4fef-a555-6fd0877dc7ca"
                                    |    },
                                    |    {
                                    |      "name": "correlationId",
                                    |      "value": "5fht738957jfjf845jgjf855"
                                    |    }
                                    |  ]
                                    |}""".stripMargin
  val rdsSubmissionResponse3 =
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
       |            "name": "feedbackId",
       |            "value": "a365c0b4-06e3-4fef-a555-6fd0877dc7c"
       |        },
       |        {
       |            "name": "calculationId",
       |            "value": "537490b4-06e3-4fef-a555-6fd0877dc7ca"
       |        },
       |        {
       |            "name": "correlationId",
       |            "value": "5fht738957jfjf845jgjf855"
       |        }
       |        ]
       |       }
       |    ]
       |}
       |
       |""".stripMargin


  val assessmentRequestForSelfAssessment = AssessmentRequestForSelfAssessment(
    calculationId = simpleCalculationID,
    nino = "AA00000B",
    preferredLanguage = PreferredLanguage.English,
    customerType = CustomerType.TaxPayer,
    agentRef = None,
    taxYear = "2022"
  )

  val fraudRiskReport = FraudRiskReport(
    score = 10,
    headers = Set(FraudRiskHeader("key", "value")),
    fraudRiskReportReasons = Set(FraudRiskReportReason("flag"))
  )

  def rdsRequest: RdsRequest =
      RdsRequest(
        Seq(
          RdsRequest.InputWithString("calculationId", assessmentRequestForSelfAssessment.calculationId.toString),
          RdsRequest.InputWithString("nino", assessmentRequestForSelfAssessment.nino),
          RdsRequest.InputWithString("taxYear", assessmentRequestForSelfAssessment.taxYear),
          RdsRequest.InputWithString("customerType", assessmentRequestForSelfAssessment.customerType.toString),
          RdsRequest.InputWithString("agentRef", assessmentRequestForSelfAssessment.agentRef.getOrElse("")),
          RdsRequest.InputWithString("preferredLanguage", assessmentRequestForSelfAssessment.preferredLanguage.toString),
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
          RdsRequest.InputWithObject("reason",
            Seq(
              MetadataWrapper(
                Seq(
                  Map("Reason" -> "string")
                )),
              DataWrapper(fraudRiskReport.fraudRiskReportReasons.map(value => Seq(value.reason)).toSeq)
            )
          )
        )
  )

  val risks = Vector(
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
    reportID =  simpleReportID,
    risks = risks,
    nino = assessmentRequestForSelfAssessment.nino,
    taxYear = DesTaxYear.fromDesIntToString(assessmentRequestForSelfAssessment.taxYear.toInt) ,
    calculationId = simpleCalculationID,
    rdsCorrelationId = "5fht738957jfjf845jgjf855"
  )
}
