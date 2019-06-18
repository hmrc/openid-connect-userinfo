/*
 * Copyright 2019 HM Revenue & Customs
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
import play.api.Mode.Mode
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.config.ServicesConfig

@Singleton
class AppContext @Inject()(override val runModeConfiguration: Configuration, environment: Environment) extends ServicesConfig {
  override protected def mode: Mode = environment.mode

  lazy val appName : String = runModeConfiguration.getString("appName").getOrElse(throw new RuntimeException("appName is not configured"))
  lazy val appUrl : String = runModeConfiguration.getString("appUrl").getOrElse(throw new RuntimeException("appUrl is not configured"))
  lazy val authUrl : String = baseUrl("auth")
  lazy val thirdPartyDelegatedAuthorityUrl : String = baseUrl("third-party-delegated-authority")
  lazy val access: Option[Configuration] = runModeConfiguration.getConfig(s"$env.api.access")
  lazy val desEnvironment: String = runModeConfiguration.getString(s"$env.microservice.services.des.environment").getOrElse(throw new RuntimeException(s"$env.microservice.services.des.environment is not configured"))
  lazy val desBearerToken: String =  runModeConfiguration.getString(s"$env.microservice.services.des.bearer-token").getOrElse(throw new RuntimeException(s"$env.microservice.services.des.bearer-token is not configured"))
  lazy val logUserInfoResponsePayload: Boolean = runModeConfiguration.underlying.getBoolean("log-user-info-response-payload")
}
