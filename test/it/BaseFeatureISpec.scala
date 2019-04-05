/*
 * Copyright 2019 HM Revenue & Customs
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

package it

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, post, stubFor, urlPathMatching}
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import org.scalatest._
import play.api.{Configuration, Logger}
import uk.gov.hmrc.play.it.{ExternalServiceRunner, MongoMicroServiceEmbeddedServer}
import scala.concurrent.duration._

abstract class BaseFeatureISpec(testName: String, additionalServices: Seq[String] = Seq.empty) extends FeatureSpec with GivenWhenThen with Matchers
  with BeforeAndAfterEach with BeforeAndAfterAll {
  lazy val additionalConfig: Seq[(String, Any)] = Seq.empty

  val stubHost = "localhost"
  val stubPort = sys.env.getOrElse("WIREMOCK_SERVICE_LOCATOR_PORT", "6008").toInt
  val wireMockServer = new WireMockServer(wireMockConfig().port(stubPort))

  val timeout = 10.second

  wireMockServer.start()
  WireMock.configureFor(stubHost, stubPort)
  stubFor(post(urlPathMatching("/registration")).willReturn(aResponse().withStatus(204)))

  val server = new IntegrationServer(testName, additionalServices, additionalConfig.toMap)

  lazy val config = Configuration.from(server.additionalConfig)

  override def beforeAll() {
    super.beforeAll()
    server.start()
  }

  override def afterAll() {
    super.afterAll()
    server.stop()
  }
}

class IntegrationServer(override val testName: String, extraServicesToRun: Seq[String], extraConfig: Map[String, Any]) extends MongoMicroServiceEmbeddedServer {

  override protected val servicePort: Int = 8500

  val extraServices = extraServicesToRun map (serviceName => ExternalServiceRunner.runFromJar(serviceName))

  override val externalServices =  extraServices

  override def stop(): Unit = ()

  override lazy val additionalConfig =
    Map[String, Any](
      "application.router" -> "testOnlyDoNotUseInAppConf.Routes"
    ) ++ extraConfig
}
