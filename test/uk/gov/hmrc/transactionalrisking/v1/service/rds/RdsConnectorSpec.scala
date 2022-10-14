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

import mockws.{MockWS, MockWSHelpers}
import org.scalatest.BeforeAndAfterAll
import play.api.mvc.Results.Ok
import play.api.test.Helpers._
import uk.gov.hmrc.transactionalrisking.models.outcomes.ResponseWrapper
import uk.gov.hmrc.transactionalrisking.services.ServiceOutcome
import uk.gov.hmrc.transactionalrisking.services.rds.RdsConnector
import uk.gov.hmrc.transactionalrisking.services.rds.models.response.NewRdsAssessmentReport
import uk.gov.hmrc.transactionalrisking.support.{ConnectorSpec, MockAppConfig}
import uk.gov.hmrc.transactionalrisking.v1.CommonTestData.commonTestData.{simpleRDSCorrelationId, simpleTaxYearEndInt}
import RdsTestData.{acknowledgeReportRequest, rdsAssessmentReport, rdsRequest, rdsSubmissionResponse}
import uk.gov.hmrc.transactionalrisking.v1.CommonTestData.commonTestData


class RdsConnectorSpec extends ConnectorSpec
                          with MockAppConfig
                          with MockWSHelpers
                          with BeforeAndAfterAll {

  var port: Int = _

  val submitBaseUrl:String = s"http://localhost:$port/submit"
  val acknowledgeUrl:String = s"http://localhost:$port/acknowledge"

  val ws = MockWS {
    case (POST, submitBaseUrlTmp) if (submitBaseUrlTmp==submitBaseUrl)=>
      Action { Ok(rdsSubmissionResponse) }
    case (POST, acknowledgeUrlTmp) if (acknowledgeUrlTmp==acknowledgeUrl)  =>
      Action { Ok }
    case (_, _) =>
      throw new RuntimeException("Unable to distinguish API call or path whilst testing")
  }

  override def afterAll(): Unit = {
    shutdownHelpers()
  }

  class Test {
    val connector = new RdsConnector(ws, mockAppConfig)
  }

  "RDSConnector" when {
    "submit method is called" must {
      "return the response if successful" in new Test {
        MockedAppConfig.rdsBaseUrlForSubmit returns submitBaseUrl

        val rhs = await(connector.submit(rdsRequest))
        val lhs =  Right(ResponseWrapper(simpleRDSCorrelationId, rdsAssessmentReport))

        lhs shouldBe rhs

      }
    }

//TODO : Get working again.
//    "acknowledge method is called" must {
//      "return the response if successful" in new Test {
//        MockedAppConfig.rdsBaseUrlForAcknowledge returns acknowledgeUrl
//
//
//        val ret:ServiceOutcome[ NewRdsAssessmentReport ] = await(connector.acknowledgeRds(acknowledgeReportRequest))
//        val Right( ResponseWrapper( correlationIdRet, newRdsAssessmentReport)) = ret
//
//        val rdsCorrelationId:String = newRdsAssessmentReport.rdsCorrelationId
//        val year:Int = newRdsAssessmentReport.taxYear
//        val responceCode:Int = newRdsAssessmentReport.responseCode
//
//        correlationIdRet shouldBe commonTestData.internalCorrelationId
//
//        rdsCorrelationId shouldBe simpleRDSCorrelationId
//        year shouldBe simpleTaxYearEndInt
//        responceCode shouldBe NO_CONTENT
//
//
//
//      }
//    }
  }
}
