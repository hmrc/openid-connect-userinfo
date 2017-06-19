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
import uk.gov.hmrc.openidconnect.userinfo.config.WSHttp
import uk.gov.hmrc.openidconnect.userinfo.connectors.AuthConnector
import uk.gov.hmrc.openidconnect.userinfo.domain.{Authority, Enrolment, EnrolmentIdentifier}
import uk.gov.hmrc.play.auth.microservice.connectors.ConfidenceLevel
import uk.gov.hmrc.play.auth.microservice.connectors.ConfidenceLevel._
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet}
import unit.uk.gov.hmrc.openidconnect.userinfo.WireMockSugar

class AuthConnectorSpec extends WireMockSugar {

//  "confidenceLevel" should {
//    "be the authority's confidence level" in new TestAuthConnector(wiremockBaseUrl) {
//      given().get(urlPathEqualTo("/auth/authority")).returns(authorityJson(L200))
//      confidenceLevel().futureValue shouldBe Some(200)
//    }
//
//    "return None when authority's confidence level is not in the response" in new TestAuthConnector(wiremockBaseUrl) {
//      given().get(urlPathEqualTo("/auth/authority")).returns("""{"credentialStrength":"weak"}""")
//      confidenceLevel().futureValue shouldBe None
//    }
//
//    "return false when auth request fails" in new TestAuthConnector(wiremockBaseUrl) {
//      given().get(urlPathEqualTo("/auth/authority")).returns(500)
//      confidenceLevel().futureValue shouldBe None
//    }
//  }

//  "fetchNino" should {
//    "return the authority's nino" in new TestAuthConnector(wiremockBaseUrl) {
//      given().get(urlPathEqualTo("/auth/authority")).returns("""{"nino":"NB966669A"}""")
//      fetchNino().futureValue shouldBe Some(Nino("NB966669A"))
//    }
//
//    "return None when authority's NINO is not in the response" in new TestAuthConnector(wiremockBaseUrl) {
//      given().get(urlPathEqualTo("/auth/authority")).returns("""{"credentialStrength":"weak"}""")
//      fetchNino().futureValue shouldBe None
//    }
//
//    "return None when auth request fails" in new TestAuthConnector(wiremockBaseUrl) {
//      given().get(urlPathEqualTo("/auth/authority")).returns(500)
//      fetchNino().futureValue shouldBe None
//    }
//  }

  "fetchEnrloments" should {
    val authority = Authority(Some("weak"), Some(200), Some("AA111111A"), Some("/uri/to/userDetails"),
      Some("/uri/to/enrolments"), Some("Individual"), Some("1304372065861347"))
    "return the authority enrloments" in new TestAuthConnector(wiremockBaseUrl) {
      given().get(urlPathEqualTo("/uri/to/enrolments")).returns(enrolmentsJson())
      fetchEnrolments(authority).futureValue shouldBe Some(Seq(Enrolment("IR-SA", List(EnrolmentIdentifier("UTR", "174371121")))))
    }

    "return None when there is no URI for enrolments" in new TestAuthConnector(wiremockBaseUrl) {
      fetchEnrolments(authority.copy(enrolments = None)).futureValue shouldBe None
    }

    "return None when there are no enrolments at all" in new TestAuthConnector(wiremockBaseUrl) {
      given().get(urlPathEqualTo("/uri/to/enrolments")).returns("{}")
      fetchEnrolments(authority).futureValue shouldBe None
    }
  }

  "fetchAuthority" should {
    "return the authority with some GG fields" in new TestAuthConnector(wiremockBaseUrl) {
      given().get(urlPathEqualTo("/auth/authority")).returns(authorityJson(L200))
      fetchAuthority().futureValue shouldBe Some(Authority(Some("weak"),Some(200),Some("AB123456A"),Some("/uri/to/userDetails"),
        Some("/uri/to/enrolments"),Some("Individual"),Some("1304372065861347")))
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
         |    "nino": "AB123456A",
         |    "userDetailsLink": "/uri/to/userDetails",
         |    "enrolments": "/uri/to/enrolments",
         |    "affinityGroup": "Individual",
         |    "credId": "1304372065861347"
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
