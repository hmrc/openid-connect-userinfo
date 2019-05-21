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

package unit.uk.gov.hmrc.openidconnect.userinfo.config

import play.api.Configuration
import uk.gov.hmrc.openidconnect.userinfo.config.{APIAccessConfig, APIAccessVersions}
import uk.gov.hmrc.play.test.UnitSpec

class APIAccessConfigSpec extends UnitSpec {
  val configuration = Configuration.from(Map(
    "api.access.version.1_0.type" -> "PRIVATE",
    "api.access.version.1_0.status" -> "STABLE",
    "api.access.version.1_0.white-list.applicationIds.0" -> "649def0f-3ed3-4df5-8ae1-3e687a9143ea",
    "api.access.version.1_0.white-list.applicationIds.1" -> "df8c10db-01fb-4543-b77e-859267462231",
    "api.access.version.1_0.white-list.applicationIds.2" -> "9a32c713-7741-4aae-b39d-957021fb97a9",
    "api.access.version.1_0.endpointsEnabled" -> true,
    "api.access.version.1_1.type" -> "PRIVATE",
    "api.access.version.1_1.status" -> "ALPHA",
    "api.access.version.1_1.white-list.applicationIds.0" -> "649def0f-3ed3-4df5-8ae1-3e687a9143ea",
    "api.access.version.1_1.endpointsEnabled" -> false
  ))

  "API Access Versions" should {
    "determine whether or not a version is enabled in the current environment" in {
      val apiVersions = new APIAccessVersions(configuration.getObject("api.access.version"))
      apiVersions.versions.getOrElse(List()).size shouldBe 2

      apiVersions.versions.get.map {version =>
        version shouldBe a [APIAccessConfig]
        version.version match {
          case "1.0" =>
            version.accessType shouldBe "PRIVATE"
            version.status shouldBe "STABLE"
            version.endpointsEnabled shouldBe true
            version.whiteListedApplicationIds shouldBe a [List[String]]
            version.whiteListedApplicationIds.size shouldBe 3
            version.whiteListedApplicationIds should contain ("649def0f-3ed3-4df5-8ae1-3e687a9143ea")
            version.whiteListedApplicationIds should contain ("df8c10db-01fb-4543-b77e-859267462231")
            version.whiteListedApplicationIds should contain ("9a32c713-7741-4aae-b39d-957021fb97a9")
          case "1.1" =>
            version.accessType shouldBe "PRIVATE"
            version.status shouldBe "ALPHA"
            version.endpointsEnabled shouldBe false
            version.whiteListedApplicationIds shouldBe a [List[String]]
            version.whiteListedApplicationIds.size shouldBe 1
            version.whiteListedApplicationIds should contain ("649def0f-3ed3-4df5-8ae1-3e687a9143ea")
          case unknown => fail(s"Unknown version found : $unknown")
        }
      }
    }
  }
}
