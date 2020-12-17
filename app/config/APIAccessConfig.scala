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

import com.typesafe.config.{Config, ConfigObject}

import collection.JavaConverters._

case class APIAccessConfig(version: String, status: String, accessType: String, endpointsEnabled: Boolean, whiteListedApplicationIds: List[String])

case class APIAccessVersions(versionConfigs: Option[ConfigObject]) {
  def findAPIs(versions: List[String], config: Config) = {
    versions.map { version =>
      val value = config.getConfig(version)

      val accessType = if (value.hasPath("type")) value.getString("type") else "PRIVATE"
      val status = if (value.hasPath("status")) value.getString("status") else throw new IllegalArgumentException("Status missing")
      val allowListedApplicationIds = if (value.hasPath("white-list.applicationIds")) Some(value.getStringList("white-list.applicationIds").asScala.toList) else None
      val endpointsEnabled = if (value.hasPath("endpointsEnabled")) value.getBoolean("endpointsEnabled") else false
      val versionNumber = version.replace('_', '.')

      new APIAccessConfig(versionNumber, status, accessType, endpointsEnabled, allowListedApplicationIds.getOrElse(List()))
    }
  }

  def versions = {
    for {
      config <- versionConfigs
      keys = config.unwrapped().keySet().asScala.toList
      api = findAPIs(keys, config.toConfig)
    } yield api
  }
}
