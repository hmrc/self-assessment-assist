/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.selfassessmentassist.config

import com.google.inject.{AbstractModule, Provides}
import org.apache.pekko.actor.{ActorSystem, Scheduler}
import play.api.Configuration
import play.api.libs.ws.{WSClient, WSProxyServer}
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.http.hooks.HttpHook
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import uk.gov.hmrc.play.http.ws.{WSProxy, WSProxyConfiguration}
import uk.gov.hmrc.selfassessmentassist.v1.schedulers.NrsSubmissionScheduler

import javax.inject.Named

class Module extends AbstractModule {

  override def configure(): Unit = {

    bind(classOf[AppConfig]).to(classOf[AppConfigImpl]).asEagerSingleton()
    bind(classOf[NrsSubmissionScheduler]).asEagerSingleton()
  }

  @Provides
  def akkaScheduler(actorSystem: ActorSystem): Scheduler =
    actorSystem.scheduler

  import com.google.inject.Provides

  @Provides
  @Named("external-http-client")
  def provideExternalHttpClient(
      auditConnector: HttpAuditing,
      wsClient: WSClient,
      actorSystem: ActorSystem,
      config: Configuration
  ): HttpClient =
    new DefaultHttpClient(config, auditConnector, wsClient, actorSystem) with WSProxy {

      override def wsProxyServer: Option[WSProxyServer] =
        WSProxyConfiguration.buildWsProxyServer(config)

    }

  @Provides
  @Named("nohook-auth-http-client")
  def authExternalHttpClient(
      auditConnector: HttpAuditing,
      wsClient: WSClient,
      actorSystem: ActorSystem,
      config: Configuration
  ): HttpClient =
    new DefaultHttpClient(config, auditConnector, wsClient, actorSystem) with WSProxy {
      override val hooks: Seq[HttpHook] = NoneRequired

      override def wsProxyServer: Option[WSProxyServer] = WSProxyConfiguration.buildWsProxyServer(config)
    }

}
