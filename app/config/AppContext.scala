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

package config

import javax.inject.{Inject, Singleton}
import com.typesafe.config.ConfigObject
import play.api.Mode
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.bootstrap.config.{RunMode, ServicesConfig}

@Singleton
class AppContext @Inject() (val runModeConfiguration: Configuration, environment: Environment, runMode: RunMode) extends ServicesConfig(runModeConfiguration, runMode) {
  protected def mode: Mode = environment.mode

  lazy val appName: String = runModeConfiguration.get[String]("appName")
  lazy val appUrl: String = runModeConfiguration.get[String]("appUrl")
  lazy val authUrl: String = baseUrl("auth")
  lazy val thirdPartyDelegatedAuthorityUrl: String = baseUrl("third-party-delegated-authority")
  lazy val access: Option[ConfigObject] = runModeConfiguration.getOptional[ConfigObject]("api.access.version")
  lazy val desEnvironment: String = runModeConfiguration.get[String](s"$runMode.env.microservice.services.des.environment")
  lazy val desBearerToken: String = runModeConfiguration.get[String](s"$runMode.env.microservice.services.des.bearer-token")
  lazy val logUserInfoResponsePayload: Boolean = runModeConfiguration.underlying.getBoolean("log-user-info-response-payload")
}
