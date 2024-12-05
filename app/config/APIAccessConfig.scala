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

import com.typesafe.config.{Config, ConfigObject}

import scala.jdk.CollectionConverters.*

case class APIAccessConfig(version: String, status: String, accessType: String, endpointsEnabled: Boolean, allowListedApplicationIds: List[String])

case class APIAccessVersions(versionConfigs: Option[ConfigObject]) {
  def findAPIs(versions: List[String], config: Config): List[APIAccessConfig] = {
    versions.map { version =>
      val value = config.getConfig(version)

      val accessType = if (value.hasPath("type")) value.getString("type") else "PRIVATE"
      val status = if (value.hasPath("status")) value.getString("status") else throw new IllegalArgumentException("Status missing")
      val allowListedApplicationIds =
        if (value.hasPath("allow-list.applicationIds")) Some(value.getStringList("allow-list.applicationIds").asScala.toList) else None
      val endpointsEnabled = if (value.hasPath("endpointsEnabled")) value.getBoolean("endpointsEnabled") else false
      val versionNumber = version.replace('_', '.')

      APIAccessConfig(versionNumber, status, accessType, endpointsEnabled, allowListedApplicationIds.getOrElse(List()))
    }
  }

  def versions: Option[List[APIAccessConfig]] = {
    for {
      config <- versionConfigs
      keys = config.unwrapped().keySet().asScala.toList
      api = findAPIs(keys, config.toConfig)
    } yield api
  }
}
