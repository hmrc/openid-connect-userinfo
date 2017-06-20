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
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import uk.gov.hmrc.openidconnect.userinfo.config.WSHttp
import uk.gov.hmrc.openidconnect.userinfo.connectors.UserDetailsConnector
import uk.gov.hmrc.openidconnect.userinfo.domain.{Authority, UserDetails}
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet}
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import unit.uk.gov.hmrc.openidconnect.userinfo.WiremockDSL

class UserDetailsConnectorSpec extends UnitSpec with BeforeAndAfterAll with WithFakeApplication with WiremockDSL with ScalaFutures {
  val stubPort = sys.env.getOrElse("WIREMOCK", "11111").toInt
  val stubHost = "localhost"
  val wireMockUrl = s"http://$stubHost:$stubPort"
  val wireMockServerUserDetails = new WireMockServer(wireMockConfig().port(stubPort))

  trait Setup {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val connector = new UserDetailsConnector {
      override val http: HttpGet = WSHttp
    }

    def userDetailsJson() = {
      s"""
         |{
         |   "affinityGroup": "Individual",
         |   "credentialRole": "Admin",
         |   "email": "fst.snd@name.com"
         |}
        """.stripMargin
    }
  }

  override def beforeAll() {
    super.beforeAll()
    wireMockServerUserDetails.start()
    configureFor(stubHost, stubPort)
  }

  override def afterAll() {
    super.afterAll()
    wireMockServerUserDetails.resetMappings()
    wireMockServerUserDetails.stop()
  }

  "fetchUserDetails" should {
    val userDetailsPath = "/uri/to/userDetails"
    val userDetailsUri = s"$wireMockUrl$userDetailsPath"
    val authority: Authority = Authority(Some("weak"), Some(200), Some("AA111111A"), Some(userDetailsUri),
      Some("/uri/to/enrolments"), Some("Individual"), Some("1304372065861347"))

    "return user details" in new Setup() {
      given().get(urlPathEqualTo(s"$userDetailsPath")).returns(userDetailsJson())
      connector.fetchUserDetails(authority).futureValue shouldBe Some(UserDetails(affinityGroup = Some("Individual"), credentialRole = Some("Admin"), email = Some("fst.snd@name.com")))
    }

    "return user details with affinity group only" in new Setup() {
      given().get(urlPathEqualTo(s"$userDetailsPath")).returns("""{"affinityGroup": "Individual"}""")
      connector.fetchUserDetails(authority).futureValue shouldBe Some(UserDetails(affinityGroup = Some("Individual")))
    }

    "return user details with credential role only" in new Setup() {
      given().get(urlPathEqualTo(s"$userDetailsPath")).returns("""{"credentialRole": "Admin"}""")
      connector.fetchUserDetails(authority).futureValue shouldBe Some(UserDetails(credentialRole = Some("Admin")))
    }

    "return user empty UserDetails if there are no user details" in new Setup() {
      given().get(urlPathEqualTo(s"$userDetailsPath")).returns("{}")
      connector.fetchUserDetails(authority).futureValue shouldBe Some(UserDetails())
    }
  }
}
