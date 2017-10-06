/*
 * Copyright 2017 HM Revenue & Customs
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

package unit.uk.gov.hmrc.openidconnect.userinfo.connectors

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import org.scalatest.BeforeAndAfterEach
import uk.gov.hmrc.openidconnect.userinfo.config.WSHttp
import uk.gov.hmrc.openidconnect.userinfo.connectors.ThirdPartyDelegatedAuthorityConnector
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import uk.gov.hmrc.http.{ HeaderCarrier, HttpGet }

class ThirdPartyDelegatedAuthorityConnectorSpec extends UnitSpec with BeforeAndAfterEach with WithFakeApplication {

  val stubPort = sys.env.getOrElse("WIREMOCK", "11111").toInt
  val stubHost = "localhost"
  val wireMockUrl = s"http://$stubHost:$stubPort"
  val wireMockServer = new WireMockServer(wireMockConfig().port(stubPort))

  trait Setup {
    implicit val hc = HeaderCarrier()

    val connector = new ThirdPartyDelegatedAuthorityConnector {
      override val serviceUrl: String = wireMockUrl
      override val http: HttpGet = WSHttp
    }
  }

  override def beforeEach() {
    wireMockServer.start()
    WireMock.configureFor(stubHost, stubPort)
  }

  override def afterEach() {
    wireMockServer.resetMappings()
    wireMockServer.stop()
  }

  "fetchScopes" should {

    "return the scopes of the delegated authority" in new Setup {
      val authBearerToken = "AUTH_TOKEN"

      stubFor(get(urlPathMatching(s"/delegated-authority")).withQueryParam("auth_bearer_token", equalTo(authBearerToken)).
        willReturn(
          aResponse()
          .withStatus(200)
          .withBody(
            s"""
               |{
               |  "token": {
               |    "scopes": ["scope1", "scope2"]
               |  }
               |}
            """.stripMargin)))

      val scopes = await(connector.fetchScopes(authBearerToken))

      scopes shouldBe Set("scope1", "scope2")
    }

    "return an empty set when delegated authority is not found" in new Setup {
      val authBearerToken = "AUTH_TOKEN"

      stubFor(get(urlPathMatching(s"/delegated-authority")).withQueryParam("auth_bearer_token", equalTo(authBearerToken)).
        willReturn(aResponse().withStatus(404)))

      val scopes = await(connector.fetchScopes(authBearerToken))

      scopes shouldBe Set()
    }
  }
}
