/*
 * Copyright 2023 HM Revenue & Customs
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
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import config.AppContext
import org.mockito.scalatest.MockitoSugar
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import scala.concurrent.ExecutionContext.Implicits.global
import testSupport.UnitSpec

class ThirdPartyDelegatedAuthorityConnectorSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach with GuiceOneAppPerSuite {

  val stubPort: Int = sys.env.getOrElse("WIREMOCK", "11111").toInt
  val stubHost = "localhost"
  val wireMockUrl = s"http://$stubHost:$stubPort"
  val wireMockServer = new WireMockServer(wireMockConfig().port(stubPort))

  trait Setup {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val accessToken = "ACCESS_TOKEN"

    val mockAppContext: AppContext = mock[AppContext]

    when(mockAppContext.thirdPartyDelegatedAuthorityUrl).thenReturn(wireMockUrl)

    val httpClient: HttpClient = app.injector.instanceOf[HttpClient]
    val connector:  ThirdPartyDelegatedAuthorityConnector = new ThirdPartyDelegatedAuthorityConnector(mockAppContext, httpClient)
  }

  override def beforeEach(): Unit = {
    wireMockServer.start()
    WireMock.configureFor(stubHost, stubPort)
  }

  override def afterEach(): Unit = {
    wireMockServer.resetMappings()
    wireMockServer.stop()
  }

  "fetchScopes" should {

    "return the scopes of the delegated authority" in new Setup {

      stubFor(
        get(urlPathMatching(s"/delegated-authority"))
          .withHeader("access-token", equalTo(accessToken))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody(s"""
               |{
               |  "token": {
               |    "scopes": ["scope1", "scope2"]
               |  }
               |}
            """.stripMargin)
          )
      )

      val scopes: Set[String] = await(connector.fetchScopes(accessToken))

      scopes shouldBe Set("scope1", "scope2")
    }

    "return an empty set when delegated authority is not found" in new Setup {

      stubFor(
        get(urlPathMatching(s"/delegated-authority"))
          .withHeader("access-token", equalTo(accessToken))
          .willReturn(aResponse().withStatus(404))
      )

      val scopes: Set[String] = await(connector.fetchScopes(accessToken))

      scopes shouldBe Set()
    }
  }
}
