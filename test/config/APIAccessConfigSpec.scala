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
import play.api.Configuration
import testSupport.UnitSpec

class APIAccessConfigSpec extends UnitSpec {
  val configuration: Configuration = Configuration.from(
    Map(
      "api.access.version.1_0.type"             -> "PRIVATE",
      "api.access.version.1_0.status"           -> "STABLE",
      "api.access.version.1_0.endpointsEnabled" -> true
    )
  )

  "API Access Versions" should {
    "determine whether or not a version is enabled in the current environment" in {
      val apiVersions = APIAccessVersions(configuration.getOptional[ConfigObject]("api.access.version"))
      apiVersions.versions.getOrElse(List()).size shouldBe 1

      apiVersions.versions.get.map { version =>
        version shouldBe a[APIAccessConfig]
        version.version match {
          case "1.0" =>
            version.accessType       shouldBe "PRIVATE"
            version.status           shouldBe "STABLE"
            version.endpointsEnabled shouldBe true
          case unknown => fail(s"Unknown version found : $unknown")
        }
      }
    }
  }
}
