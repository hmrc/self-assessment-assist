package uk.gov.hmrc.selfassessmentassist.utils

import uk.gov.hmrc.selfassessmentassist.support.UnitSpec

import java.time.OffsetDateTime

class CurrentDateTimeSpec extends UnitSpec {
  val currentDateTime = new CurrentDateTime()

  "getDateTime" should {
    "return a correctly formatted date time string" in {
      val timeUnderTest = OffsetDateTime.MIN.withYear(1999)
      currentDateTime.dateString(timeUnderTest).toString shouldBe "1999-01-01T00:00Z"

    }
  }

}
