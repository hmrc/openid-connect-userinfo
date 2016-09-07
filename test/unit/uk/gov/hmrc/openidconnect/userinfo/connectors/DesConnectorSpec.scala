/*
 * Copyright 2016 HM Revenue & Customs
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
import org.joda.time.LocalDate
import org.scalatest.BeforeAndAfterEach
import uk.gov.hmrc.openidconnect.userinfo.config.WSHttp
import uk.gov.hmrc.openidconnect.userinfo.connectors.DesConnector
import uk.gov.hmrc.openidconnect.userinfo.domain._
import uk.gov.hmrc.play.http.{Upstream5xxResponse, HeaderCarrier, HttpGet}
import uk.gov.hmrc.play.test.{WithFakeApplication, UnitSpec}

class DesConnectorSpec extends UnitSpec with BeforeAndAfterEach with WithFakeApplication {

  val stubPort = sys.env.getOrElse("WIREMOCK", "11111").toInt
  val stubHost = "localhost"
  val wireMockUrl = s"http://$stubHost:$stubPort"
  val wireMockServer = new WireMockServer(wireMockConfig().port(stubPort))
  val desEnv = "local"
  val desToken = "aToken"

  trait Setup {
    implicit val hc = HeaderCarrier()

    val connector = new DesConnector {
      override val serviceUrl: String = wireMockUrl
      override val desEnvironment: String = desEnv
      override val desBearerToken: String = desToken
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

  "fetch user info" should {
    val nino = "AB123456"

    "return the user info" in new Setup {

      stubFor(get(urlPathMatching(s"/pay-as-you-earn/individuals/$nino"))
        .withHeader("Authorization", equalTo(s"Bearer $desToken"))
        .withHeader("Environment", equalTo(desEnv)).willReturn(
        aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/json")
          .withBody(
            s"""
              |{
              |  "nino": "$nino",
              |  "names": {
              |    "1": {
              |      "firstForenameOrInitial": "Andrew",
              |      "secondForenameOrInitial": "John",
              |      "surname": "Smith"
              |    }
              |  },
              |  "sex": "M",
              |  "dateOfBirth": "1980-01-01",
              |  "addresses": {
              |    "1": {
              |      "line1": "1 Station Road",
              |      "line2": "Town Centre",
              |      "line3": "Sometown",
              |      "line4": "Anyshire",
              |      "postcode": "AB12 3CD",
              |      "countryCode": 1
              |    }
              |  }
              |}
              |
            """.stripMargin)))

      val result = await(connector.fetchUserInfo(nino))

      result shouldBe DesUserInfo("AB123456",
        DesUserName("Andrew", Some("John"), "Smith"),
        Some(LocalDate.parse("1980-01-01")),
        DesAddress("1 Station Road", "Town Centre", Some("Sometown"), Some("Anyshire"), Some("AB12 3CD"), Some(1)))
    }

    "fail with UserInfoNotFound when DES does not have an entry for the NINO" in new Setup {

      stubFor(get(urlPathMatching(s"/pay-as-you-earn/individuals/$nino")).willReturn(
        aResponse().withStatus(404)))

      intercept[UserInfoNotFoundException]{await(connector.fetchUserInfo(nino))}
    }

    "fail with UserInfoNotFound when DES does not have validated data" in new Setup {

      stubFor(get(urlPathMatching(s"/pay-as-you-earn/individuals/$nino")).willReturn(
        aResponse().withStatus(400)))

      intercept[UserInfoNotFoundException]{await(connector.fetchUserInfo(nino))}
    }

    "fail when DES returns a 500 response" in new Setup {

      stubFor(get(urlPathMatching(s"/pay-as-you-earn/individuals/$nino")).willReturn(
        aResponse().withStatus(500)))

      intercept[Upstream5xxResponse]{await(connector.fetchUserInfo(nino))}
    }
  }
}
