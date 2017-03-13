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

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.openidconnect.userinfo.config.WSHttp
import uk.gov.hmrc.openidconnect.userinfo.connectors.AuthConnector
import uk.gov.hmrc.openidconnect.userinfo.domain.{Enrolment, EnrolmentIdentifier, NinoNotFoundException}
import uk.gov.hmrc.play.auth.microservice.connectors.ConfidenceLevel
import uk.gov.hmrc.play.auth.microservice.connectors.ConfidenceLevel._
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, Upstream5xxResponse}
import unit.uk.gov.hmrc.openidconnect.userinfo.WireMockSugar

class AuthConnectorSpec extends WireMockSugar {

  "confidenceLevel" should {
    "return the authority's confidence level" in new TestAuthConnector(wiremockBaseUrl) {
      given().get(urlPathEqualTo("/auth/authority")).returns(authorityJson(L200))
      confidenceLevel().futureValue shouldBe Some(200)
    }

    "return None when authority's confidence level is not in the response" in new TestAuthConnector(wiremockBaseUrl) {
      given().get(urlPathEqualTo("/auth/authority")).returns("""{"credentialStrength":"weak"}""")
      confidenceLevel().futureValue shouldBe None
    }

    "return false when auth request fails" in new TestAuthConnector(wiremockBaseUrl) {
      given().get(urlPathEqualTo("/auth/authority")).returns(500)
      confidenceLevel().futureValue shouldBe None
    }
  }

  "fetchNino" should {
    "return the authority's nino" in new TestAuthConnector(wiremockBaseUrl) {
      given().get(urlPathEqualTo("/auth/authority")).returns("""{"nino":"NB966669A"}""")
      fetchNino().futureValue shouldBe Nino("NB966669A")
    }

    "fail with NinoNotFoundException when authority's NINO is not in the response" in new TestAuthConnector(wiremockBaseUrl) {
      given().get(urlPathEqualTo("/auth/authority")).returns("""{"credentialStrength":"weak"}""")
      intercept[NinoNotFoundException]{await(fetchNino())}
    }

    "fail when auth request fails" in new TestAuthConnector(wiremockBaseUrl) {
      given().get(urlPathEqualTo("/auth/authority")).returns(500)
      intercept[Upstream5xxResponse]{await(fetchNino())}
    }
  }

  "fetchEnrloments" should {
    "return the authority enrloments" in new TestAuthConnector(wiremockBaseUrl) {
      given().get(urlPathEqualTo("/auth/authority")).returns(authorityJson(L200))
      given().get(urlPathEqualTo("/uri/to/enrolments")).returns(enrolmentsJson())
      fetchEnrolments().futureValue shouldBe Some(Seq(Enrolment("IR-SA", List(EnrolmentIdentifier("UTR", "174371121")))))
    }

    "return None when there is no URI for enrolments" in new TestAuthConnector(wiremockBaseUrl) {
      given().get(urlPathEqualTo("/auth/authority")).returns("""{"credentialStrength":"weak"}""")
      fetchEnrolments().futureValue shouldBe None
    }

    "return None when there are no enrolments at all" in new TestAuthConnector(wiremockBaseUrl) {
      given().get(urlPathEqualTo("/auth/authority")).returns(authorityJson(L200))
      given().get(urlPathEqualTo("/uri/to/enrolments")).returns("{}")
      fetchEnrolments().futureValue shouldBe None
    }
  }

}

class TestAuthConnector(wiremockBaseUrl: String) extends AuthConnector with MockitoSugar {
  implicit val hc = HeaderCarrier()

  override val authBaseUrl: String = wiremockBaseUrl
  override val http: HttpGet = WSHttp

  def authorityJson(confidenceLevel: ConfidenceLevel) = {
      s"""
         |{
         |    "credentialStrength":"weak",
         |    "confidenceLevel": ${confidenceLevel.level},
         |    "enrolments": "/uri/to/enrolments"
         |}
      """.stripMargin
  }

  def enrolmentsJson() = {
    s"""
       |[
       |    {
       |        "key": "IR-SA",
       |        "identifiers": [
       |            {
       |                "key": "UTR",
       |                "value": "174371121"
       |            }
       |        ],
       |        "state": "Activated"
       |    }
       |]
     """.stripMargin
  }
}
