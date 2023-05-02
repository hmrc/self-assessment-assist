package uk.gov.hmrc.selfassessmentassist.stubs

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status.OK
import play.api.libs.json.Json
import uk.gov.hmrc.selfassessmentassist.v1.TestData.CommonTestData

object NrsStub {

  def submit(url: String): StubMapping = {
    DownstreamStub.onSuccess(DownstreamStub.POST, url, OK, Json.toJson(CommonTestData.simpleNRSResponseAcknowledgeSubmission))
  }

}
