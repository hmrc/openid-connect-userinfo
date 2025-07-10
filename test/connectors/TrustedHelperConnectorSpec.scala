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

package connectors

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.*
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import config.AppContext
import domain.TrustedHelper
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import testSupport.UnitSpec
import uk.gov.hmrc.http.client.HttpClientV2

class TrustedHelperConnectorSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach with GuiceOneAppPerSuite {

  val stubPort: Int = sys.env.getOrElse("WIREMOCK", "11111").toInt
  val stubHost = "localhost"
  val wireMockUrl: String = s"http://$stubHost:$stubPort"
  val wireMockServer = new WireMockServer(wireMockConfig().port(stubPort))

  trait Setup {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val mockAppContext: AppContext = mock[AppContext]
    when(mockAppContext.fandfUrl).thenReturn(wireMockUrl)
    when(mockAppContext.platformHost).thenReturn("http://www.tax.service.gov.uk")

    val httpClient: HttpClientV2 = app.injector.instanceOf[HttpClientV2]
    val connector: TrustedHelperConnector = new TrustedHelperConnector(mockAppContext, httpClient)

    val testTrustedHelper = TrustedHelper(
      principalName = "John Smith",
      attorneyName  = "Jane Doe",
      returnLinkUrl = "/trusted-helpers/redirect-to-trusted-helpers",
      principalNino = "AA000001A"
    )
    val expectedTrustedHelper = testTrustedHelper.copy(returnLinkUrl = "http://www.tax.service.gov.uk/trusted-helpers/redirect-to-trusted-helpers")
    val absoluteTrustedHelper = testTrustedHelper.copy(returnLinkUrl = "https://www.tax.service.gov.uk/redirect")
  }

  override def beforeEach(): Unit = {
    wireMockServer.start()
    WireMock.configureFor(stubHost, stubPort)
  }

  override def afterEach(): Unit = {
    wireMockServer.resetMappings()
    wireMockServer.stop()
  }

  "getDelegation" should {

    "return Some(TrustedHelper) when delegation exists and returnLinkUrl is relative" in new Setup {
      stubFor(
        get(urlPathMatching("/fandf/delegation/get"))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody(Json.toJson(testTrustedHelper).toString())
          )
      )

      val result = await(connector.getDelegation())

      result shouldBe Some(expectedTrustedHelper)
    }

    "return Some(TrustedHelper) when delegation exists and returnLinkUrl is already absolute" in new Setup {
      stubFor(
        get(urlPathMatching("/fandf/delegation/get"))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody(Json.toJson(absoluteTrustedHelper).toString())
          )
      )

      val result = await(connector.getDelegation())

      result shouldBe Some(absoluteTrustedHelper)
    }

    "return None when delegation is not found" in new Setup {
      stubFor(
        get(urlPathMatching("/fandf/delegation/get"))
          .willReturn(aResponse().withStatus(404))
      )

      val result = await(connector.getDelegation())

      result shouldBe None
    }

    "throw an exception for unexpected response" in new Setup {
      stubFor(
        get(urlPathMatching("/fandf/delegation/get"))
          .willReturn(aResponse().withStatus(500))
      )

      intercept[Exception] {
        await(connector.getDelegation())
      }.getMessage shouldBe "Unexpected response from trusted helper service: 500"
    }
  }
}
