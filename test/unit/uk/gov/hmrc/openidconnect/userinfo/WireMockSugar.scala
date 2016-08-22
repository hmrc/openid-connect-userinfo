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

package unit.uk.gov.hmrc.openidconnect.userinfo

import java.util.concurrent.TimeUnit

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatestplus.play.OneServerPerSuite
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.duration.FiniteDuration

trait WireMockSugar extends UnitSpec with OneServerPerSuite with Eventually with ScalaFutures
  with IntegrationPatience with BeforeAndAfterAll with WiremockDSL {

  override implicit val defaultTimeout = FiniteDuration(100, TimeUnit.SECONDS)

  val WIREMOCK_PORT = 22222
  val stubHost = "localhost"

  protected val wiremockBaseUrl: String = s"http://$stubHost:$WIREMOCK_PORT"
  private val wireMockServer = new WireMockServer(wireMockConfig().port(WIREMOCK_PORT))

  override def beforeAll() = {
    super.beforeAll()
    wireMockServer.stop()
    wireMockServer.start()
    WireMock.configureFor(stubHost, WIREMOCK_PORT)
    // the below stub is here so that the application finds the registration endpoint which is called on startup
    stubFor(post(urlMatching("/registration")).willReturn(aResponse().withStatus(200)))
  }

  override def afterAll() = {
    super.afterAll()
    wireMockServer.stop()
  }
}
