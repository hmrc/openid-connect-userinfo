/*
 * Copyright 2024 HM Revenue & Customs
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

import com.typesafe.config.ConfigObject
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@Singleton
class AppContext @Inject() (val runModeConfiguration: Configuration) extends ServicesConfig(runModeConfiguration) {
  lazy val appName: String = runModeConfiguration.get[String]("appName")
  lazy val authUrl: String = baseUrl("auth")
  lazy val thirdPartyDelegatedAuthorityUrl: String = baseUrl("third-party-delegated-authority")
  lazy val fandfUrl: String = baseUrl("fandf")
  lazy val access: Option[ConfigObject] = runModeConfiguration.getOptional[ConfigObject]("api.access.version")
  lazy val logUserInfoResponsePayload: Boolean = runModeConfiguration.underlying.getBoolean("log-user-info-response-payload")
}
