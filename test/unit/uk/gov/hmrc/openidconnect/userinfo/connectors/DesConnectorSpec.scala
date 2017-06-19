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
import org.joda.time.LocalDate
import org.scalatest.BeforeAndAfterEach
import uk.gov.hmrc.openidconnect.userinfo.config.WSHttp
import uk.gov.hmrc.openidconnect.userinfo.connectors.DesConnector
import uk.gov.hmrc.openidconnect.userinfo.domain._
import uk.gov.hmrc.play.http.logging.Authorization
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, Upstream5xxResponse}
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.collection.JavaConverters._

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
    val nino = Some("AA111111A")
    val ninoWithoutSuffix = "AA111111"
    val authority: Authority = Authority(Some("weak"), Some(200), nino, Some("/uri/to/userDetails"),
      Some("/uri/to/enrolments"), Some("Individual"), Some("1304372065861347"))

    "return the user info" in new Setup {

      stubFor(get(urlPathMatching(s"/pay-as-you-earn/individuals/$ninoWithoutSuffix"))
        .withHeader("Authorization", equalTo(s"Bearer $desToken"))
        .withHeader("Environment", equalTo(desEnv)).willReturn(
        aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/json")
          .withBody(
            s"""
              |{
              |  "nino": "$ninoWithoutSuffix",
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

      val result = await(connector.fetchUserInfo(authority))

      result shouldBe Some(DesUserInfo(
        DesUserName(Some("Andrew"), Some("John"), Some("Smith")),
        Some(LocalDate.parse("1980-01-01")),
        DesAddress(Some("1 Station Road"), Some("Town Centre"), Some("Sometown"), Some("Anyshire"), Some("AB12 3CD"), Some(1))))
    }

    "replace the Auth Authorization header by Des Authorization header" in new Setup {

      val headerCarrierWithAuthBearerToken = hc.copy(authorization = Some(Authorization("auth_bearer_token")))

      await(connector.fetchUserInfo(authority)(headerCarrierWithAuthBearerToken))

      val requestToDes = findAll(getRequestedFor(urlEqualTo(s"/pay-as-you-earn/individuals/$ninoWithoutSuffix"))).get(0)
      requestToDes.getHeaders.getHeader("Authorization").values().asScala shouldBe List("Bearer aToken")
    }

    "return None when DES does not have an entry for the NINO" in new Setup {

      stubFor(get(urlPathMatching(s"/pay-as-you-earn/individuals/$ninoWithoutSuffix")).willReturn(
        aResponse().withStatus(404)))

      val result = await(connector.fetchUserInfo(authority))

      result shouldBe None
    }

    "return None when DES does not have validated data" in new Setup {

      stubFor(get(urlPathMatching(s"/pay-as-you-earn/individuals/$ninoWithoutSuffix")).willReturn(
        aResponse().withStatus(400)))

      val result = await(connector.fetchUserInfo(authority))

      result shouldBe None
    }

    "fail when DES returns a 500 response" in new Setup {

      stubFor(get(urlPathMatching(s"/pay-as-you-earn/individuals/$ninoWithoutSuffix")).willReturn(
        aResponse().withStatus(500)))

      intercept[Upstream5xxResponse]{await(connector.fetchUserInfo(authority))}
    }
  }
}
