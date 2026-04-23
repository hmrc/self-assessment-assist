/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.selfassessmentassist.utils

import org.apache.pekko.actor.{ActorSystem, Scheduler}
import org.scalatest.concurrent.ScalaFutures
import uk.gov.hmrc.selfassessmentassist.support.UnitSpec

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.*

class RetryingSpec extends UnitSpec with ScalaFutures {

  // ActorSystem required by Delayer
  private implicit val system: ActorSystem =
    ActorSystem("RetryingSpec")

  private val retrying = new Retrying with Delayer {

    // ✅ MUST be members of THIS object
    implicit val ec: ExecutionContext =
      system.dispatcher

    implicit val scheduler: Scheduler =
      system.scheduler

    override def delay(duration: FiniteDuration): Future[Unit] =
      Future.successful(())

  }

  "retry" should {

    "return the result immediately when no delays remain" in {
      val result =
        retrying.retry(
          delays = Nil,
          retryCondition = _ => true
        )(_ => Future.successful(42))

      whenReady(result) { value =>
        value shouldBe 42
      }
    }

    "not retry when retryCondition returns false" in {
      var attempts = 0

      val result =
        retrying.retry(
          delays = List(1.second, 1.second),
          retryCondition = _ => false
        ) { _ =>
          attempts += 1
          Future.failed(new RuntimeException("no retry"))
        }

      whenReady(result.failed) { _ =>
        attempts shouldBe 1
      }
    }
  }

}
