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

package it

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.concurrent.ScalaFutures
import play.api.libs.json._
import play.api.test.FakeRequest
import uk.gov.hmrc.api.controllers.DocumentationController
import uk.gov.hmrc.api.domain.Registration
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.ws.WSRequest

import scala.concurrent.Await
import scala.concurrent.duration._
/**
 * Testcase to verify the capability of integration with the API platform.
 *
 * 1, To integrate with API platform the service needs to register itself to the service locator by calling the /registration endpoint and providing
 * - application name
 * - application url
 *
  *
 * 2a, To expose API's to Third Party Developers, the service needs to define the APIs in a definition.json and make it available under api/definition GET endpoint
 * 2b, For all of the endpoints defined in the definition.json a documentation.xml needs to be provided and be available under api/documentation/[version]/[endpoint name] GET endpoint
 *     Example: api/documentation/1.0/Fetch-Some-Data
 *
 * See: Confluence/display/ApiPlatform/API+Platform+Architecture+with+Flows
 */
class PlatformIntegrationISpec extends BaseFeatureISpec("PlatformIntegrationISpec")
  with WSRequest
  with ScalaFutures {

  override def applicableHeaders(url: String)(implicit hc: HeaderCarrier): Seq[(String, String)] = Nil

  override lazy val additionalConfig: Seq[(String, Any)] =
    Seq(
      "run.mode" -> "Test",
      "appName" -> "application-name",
      "appUrl" -> "http://microservice-name.protected.mdtp",
      "des.individual.endpoint" -> "/pay-as-you-earn/02.00.00/individuals/",
      "api.access.version.1_0.type" -> "PRIVATE",
      "api.access.version.1_0.status" -> "STABLE",
      "api.access.version.1_0.white-list.applicationIds.0" -> "649def0f-3ed3-4df5-8ae1-3e687a9143ea",
      "api.access.version.1_0.white-list.applicationIds.1" -> "df8c10db-01fb-4543-b77e-859267462231",
      "api.access.version.1_0.white-list.applicationIds.2" -> "9a32c713-7741-4aae-b39d-957021fb97a9",
      "api.access.version.1_0.endpointsEnabled" -> true,
      "api.access.version.1_1.type" -> "PRIVATE",
      "api.access.version.1_1.status" -> "ALPHA",
      "api.access.version.1_1.white-list.applicationIds.0" -> "649def0f-3ed3-4df5-8ae1-3e687a9143ea",
      "api.access.version.1_1.endpointsEnabled" -> false,
      "Test.microservice.services.service-locator.enabled" -> true
    )

  implicit val hc = new HeaderCarrier()

  val documentationController = DocumentationController
  val request = FakeRequest()

  feature("microservice") {

    scenario("provide definition endpoint") {
      val response = Await.result(buildRequest(server.resource("/api/definition")).get(), 1 minute)
      response.status shouldBe 200
    }

    scenario("provide definition for versions 1.0 and 1.1") {
      val response = Await.result(buildRequest(server.resource("/api/definition")).get(), 1 minute)
      response.status shouldBe 200
      val jsonBody = Json.parse(response.body)

      val versions : JsArray = (jsonBody \ "api" \ "versions") match {
        case JsDefined(arr : JsArray) => arr
        case _ => fail("The definition is not correctly formatted")
      }

      versions.value.length shouldBe 2

      versions.value.foreach { version =>
        (version \ "version").get.as[String] match {
          case "1.0" =>
            val (status, endpointsEnabled, accessType, whitelistIds) = extractVersionToVerify(version)
            status shouldBe "STABLE"
            endpointsEnabled shouldBe true
            accessType shouldBe "PRIVATE"
            whitelistIds.size shouldBe 3
            whitelistIds should contain ("649def0f-3ed3-4df5-8ae1-3e687a9143ea")
            whitelistIds should contain ("df8c10db-01fb-4543-b77e-859267462231")
            whitelistIds should contain ("9a32c713-7741-4aae-b39d-957021fb97a9")
          case "1.1" =>
            val (status, endpointsEnabled, accessType, whitelistIds) = extractVersionToVerify(version)
            status shouldBe "ALPHA"
            endpointsEnabled shouldBe false
            accessType shouldBe "PRIVATE"
            whitelistIds.size shouldBe 1
            whitelistIds should contain ("649def0f-3ed3-4df5-8ae1-3e687a9143ea")
          case versionId => fail(s"An unknown version is found $versionId")
        }
      }
    }
  }

  def extractVersionToVerify(version : JsValue) = {
    val status = (version \ "status").get.as[String]
    val endpointsEnabled = (version \ "endpointsEnabled").get.as[Boolean]
    val accessType = (version \ "access" \ "type").get.as[String]
    val whitelistIds = (version \ "access" \ "whitelistedApplicationIds") match {
      case JsDefined(arr: JsArray) => arr.value.map(_.as[String]).toList
      case _ => fail("Invalid whitelisted Application Ids definition")
    }
    (status, endpointsEnabled, accessType, whitelistIds)
  }
}
