package uk.gov.hmrc.selfassessmentassist.stubs

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status.CREATED
import uk.gov.hmrc.selfassessmentassist.v1.TestData.CommonTestData

object AcknowledgeStub {

  def acknowledge(url: String): StubMapping = {
    DownstreamStub.onSuccess(DownstreamStub.POST, url, CREATED, CommonTestData.rdsAssessmentAckJson)
  }

}
