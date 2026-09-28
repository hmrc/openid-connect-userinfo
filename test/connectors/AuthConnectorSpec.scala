/*
 * Copyright 2026 HM Revenue & Customs
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
import config.AppContext
import domain.DesUserInfo
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.{Json, Writes}
import testSupport.UnitSpec
import uk.gov.hmrc.auth.core.retrieve.{ItmpAddress, ItmpName}
import uk.gov.hmrc.http.{Authorization, HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.http.client.HttpClientV2

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global

class AuthConnectorSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach with GuiceOneAppPerSuite {

  val stubPort: Int = sys.env.getOrElse("WIREMOCK", "11111").toInt
  val stubHost = "localhost"
  val wireMockUrl: String = s"http://$stubHost:$stubPort"
  val wireMockServer = new WireMockServer(wireMockConfig().port(stubPort))
  val authAuthorisePath = "/auth/authorise"

  trait Setup {
    implicit val hc: HeaderCarrier = HeaderCarrier(authorization = Some(Authorization("Bearer test-token")))

    val mockAppContext: AppContext = mock[AppContext]
    when(mockAppContext.authUrl).thenReturn(wireMockUrl)

    val httpClient: HttpClientV2 = app.injector.instanceOf[HttpClientV2]
    val connector: AuthConnector = new AuthConnectorV1(mockAppContext, httpClient)

    val itmpName: ItmpName = ItmpName(
      givenName  = Some("John"),
      middleName = Some("Hannibal"),
      familyName = Some("Smith")
    )
    given Writes[ItmpName] = Json.writes[ItmpName]

    val itmpDateOfBirth: LocalDate = LocalDate.of(1982, 11, 15)

    val itmpAddress: ItmpAddress = ItmpAddress(
      line1       = Some("221B"),
      line2       = Some("BAKER STREET"),
      line3       = Some("LONDON"),
      line4       = Some("NW1 9NT"),
      line5       = Some("United Kingdom"),
      postCode    = Some("NW1 9NT"),
      countryName = Some("United Kingdom"),
      countryCode = Some("GB")
    )
    given Writes[ItmpAddress] = Json.writes[ItmpAddress]

  }

  override def beforeEach(): Unit = {
    wireMockServer.start()
    WireMock.configureFor(stubHost, stubPort)
  }

  override def afterEach(): Unit = {
    wireMockServer.resetMappings()
    wireMockServer.stop()
  }

  "fetchDesUserInfo" should {

    "return Some(DesUserInfo) when auth returns name, DoB & address fields" in new Setup {
      stubFor(
        post(urlPathMatching(authAuthorisePath))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody(s"""{
                  |  "optionalItmpName":${Json.toJson(itmpName).toString()},
                  |   "itmpDateOfBirth":${Json.toJson(itmpDateOfBirth).toString()},
                  |   "optionalItmpAddress":${Json.toJson(itmpAddress).toString}
                  |   }""".stripMargin)
          )
      )

      val result = await(connector.fetchDesUserInfo())
      result shouldBe Some(
        DesUserInfo(
          name        = Some(itmpName),
          dateOfBirth = Some(itmpDateOfBirth),
          address     = Some(itmpAddress)
        )
      )
    }

    "return None when auth response doesn't include name, DoB & address fields" in new Setup {
      stubFor(
        post(urlPathMatching(authAuthorisePath))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody("{}")
          )
      )

      val result = await(connector.fetchDesUserInfo())
      result shouldBe None
    }

    "return None when auth returns not found" in new Setup {
      stubFor(
        post(urlPathMatching(authAuthorisePath))
          .willReturn(aResponse().withStatus(404))
      )

      val result: Option[DesUserInfo] = await(connector.fetchDesUserInfo())
      result shouldBe None
    }

    "throw an exception on unexpected response, without passing on the error detail" in new Setup {
      stubFor(
        post(urlPathMatching(authAuthorisePath))
          .willReturn(aResponse().withStatus(500).withBody("Some error detail we shouldn't expose"))
      )

      val ex: UpstreamErrorResponse = intercept[UpstreamErrorResponse] {
        await(connector.fetchDesUserInfo())
      }
      ex.getMessage shouldBe "Failed to retrieve user details"
      ex.statusCode shouldBe 500
    }
  }
}
