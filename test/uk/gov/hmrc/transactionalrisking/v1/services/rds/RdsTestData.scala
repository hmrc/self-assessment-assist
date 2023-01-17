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

package uk.gov.hmrc.transactionalrisking.v1.services.rds

import uk.gov.hmrc.transactionalrisking.v1.TestData.CommonTestData._
import uk.gov.hmrc.transactionalrisking.v1.models.domain._
import uk.gov.hmrc.transactionalrisking.v1.services.cip.models.FraudRiskReport
import uk.gov.hmrc.transactionalrisking.v1.services.rds.models.request.RdsRequest
import uk.gov.hmrc.transactionalrisking.v1.services.rds.models.request.RdsRequest.{DataWrapper, MetadataWrapper}

object RdsTestData {

  val assessmentRequestForSelfAssessment: AssessmentRequestForSelfAssessment = AssessmentRequestForSelfAssessment(
    calculationId = simpleCalculationId,
    nino = "AA00000B",
    preferredLanguage = PreferredLanguage.English,
    customerType = CustomerType.TaxPayer,
    agentRef = None,
    taxYear = "2021-22"
  )

  val fraudRiskReport: FraudRiskReport = FraudRiskReport(
    score = 10,
    riskCorrelationId= correlationId,
    reasons = Seq("flag")
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
              DataWrapper(Seq(Seq.empty))
            )
          ),
          RdsRequest.InputWithObject("reason",
            Seq(
              MetadataWrapper(
                Seq(
                  Map("Reason" -> "string")
                )),
              DataWrapper(fraudRiskReport.reasons.map(value => Seq(value)).toSeq)
            )
          )
        )
  )

  val risks: Seq[Risk] = Vector(
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

  val assessmentReport: AssessmentReport = AssessmentReport(
    reportId =  simpleReportId,
    risks = risks,
    nino = assessmentRequestForSelfAssessment.nino,
    taxYear = assessmentRequestForSelfAssessment.taxYear ,
    calculationId = simpleCalculationId,
    rdsCorrelationId = simpleRDSCorrelationId
  )
}
