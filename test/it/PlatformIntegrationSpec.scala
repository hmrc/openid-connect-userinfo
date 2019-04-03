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

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, TestData}
import org.scalatestplus.play.OneAppPerTest
import play.api.Mode
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.api.domain.Registration
import uk.gov.hmrc.openidconnect.userinfo.controllers.DocumentationController
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.play.microservice.filters.MicroserviceFilterSupport

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
class PlatformIntegrationSpec extends UnitSpec with MockitoSugar with ScalaFutures with BeforeAndAfterEach with OneAppPerTest {

  val stubHost = "localhost"
  val stubPort = sys.env.getOrElse("WIREMOCK_SERVICE_LOCATOR_PORT", "11112").toInt
  val wireMockServer = new WireMockServer(wireMockConfig().port(stubPort))

  override def newAppForTest(testData: TestData) = GuiceApplicationBuilder()
    .configure("run.mode" -> "Stub")
    .configure(Map(
      "appName" -> "application-name",
      "appUrl" -> "http://microservice-name.protected.mdtp",
      "Test.microservice.services.service-locator.host" -> stubHost,
      "Test.microservice.services.service-locator.port" -> stubPort,
      "des.individual.endpoint" -> "/pay-as-you-earn/02.00.00/individuals/"))
    .in(Mode.Test).build()

  override def beforeEach() {
    wireMockServer.start()
    WireMock.configureFor(stubHost, stubPort)
    stubFor(post(urlPathMatching("/registration")).willReturn(aResponse().withStatus(204)))
  }


  trait Setup extends MicroserviceFilterSupport {
    val documentationController = DocumentationController
    val request = FakeRequest()
  }

  "microservice" should {

    "register itelf to service-locator" in new Setup {
      def regPayloadStringFor(serviceName: String, serviceUrl: String): String =
        Json.toJson(Registration(serviceName, serviceUrl, Some(Map("third-party-api" -> "true")))).toString

      verify(1, postRequestedFor(urlMatching("/registration")).
      withHeader("content-type", equalTo("application/json")).
      withRequestBody(equalTo(regPayloadStringFor("application-name", "http://microservice-name.protected.mdtp"))))
    }

    "provide definition endpoint" in new Setup {
      val result = documentationController.definition()(request)
      status(result) shouldBe 200
    }
  }

  override protected def afterEach() = {
    wireMockServer.stop()
    wireMockServer.resetMappings()
  }
}
