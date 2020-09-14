/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.openidconnect.userinfo.config

import javax.inject.{Inject, Singleton}
import com.typesafe.config.ConfigObject
import play.api.Mode.Mode
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.bootstrap.config.{RunMode, ServicesConfig}

@Singleton
class AppContext @Inject() (val runModeConfiguration: Configuration, environment: Environment, runMode: RunMode) extends ServicesConfig(runModeConfiguration, runMode) {
  protected def mode: Mode = environment.mode

  lazy val appName: String = runModeConfiguration.getString("appName").getOrElse(throw new RuntimeException("appName is not configured"))
  lazy val appUrl: String = runModeConfiguration.getString("appUrl").getOrElse(throw new RuntimeException("appUrl is not configured"))
  lazy val authUrl: String = baseUrl("auth")
  lazy val thirdPartyDelegatedAuthorityUrl: String = baseUrl("third-party-delegated-authority")
  lazy val access: Option[ConfigObject] = runModeConfiguration.getObject("api.access.version")
  lazy val desEnvironment: String = runModeConfiguration.getString(s"$runMode.env.microservice.services.des.environment").getOrElse(throw new RuntimeException(s"$runMode.env.microservice.services.des.environment is not configured"))
  lazy val desBearerToken: String = runModeConfiguration.getString(s"$runMode.env.microservice.services.des.bearer-token").getOrElse(throw new RuntimeException(s"$runMode.env.microservice.services.des.bearer-token is not configured"))
  lazy val logUserInfoResponsePayload: Boolean = runModeConfiguration.underlying.getBoolean("log-user-info-response-payload")
}
