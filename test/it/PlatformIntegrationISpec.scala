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
import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.api.controllers.DocumentationController
import uk.gov.hmrc.api.domain.Registration
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.ws.WSRequest

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
      "Test.microservice.services.service-locator.enabled" -> true
    )

  implicit val hc = new HeaderCarrier()

  val documentationController = DocumentationController
  val request = FakeRequest()

  feature("microservice") {

    scenario("provide definition endpoint") {
      val response = buildRequest(server.resource("/api/definition")).get().futureValue
      response.status shouldBe 200
    }
  }
}
