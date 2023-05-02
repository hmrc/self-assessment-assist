package uk.gov.hmrc.selfassessmentassist.stubs

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status.NO_CONTENT

object IfsServiceStub {

  def submit(url: String): StubMapping = DownstreamStub.onSuccess(DownstreamStub.POST, url, NO_CONTENT)

}
