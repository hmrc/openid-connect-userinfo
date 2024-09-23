/*
 * Copyright 2024 HM Revenue & Customs
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

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, post, stubFor, urlPathMatching}
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.*
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder

import scala.concurrent.duration.{Duration, *}
import scala.concurrent.{Await, Future}
import scala.jdk.CollectionConverters.*
import scala.util.Try

trait BaseFeatureISpec
    extends AnyFeatureSpec
    with GivenWhenThen
    with Matchers
    with GuiceOneServerPerSuite
    with BeforeAndAfterEach
    with BeforeAndAfterAll {

  implicit val timeout: Duration = 1.minutes

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .configure(
      flatStructurePortMapping()
        ++ extraConfig
    )
    .build()

  protected def resource(resource: String) = s"http://localhost:$port$resource"

  def await[A](future: Future[A])(implicit timeout: Duration): A = Await.result(future, timeout)

  private def flatStructurePortMapping(): Map[String, Int] = {
    lazy val config: Config = ConfigFactory.load()
    Try(config.getConfig("microservice.services"))
      .getOrElse(ConfigFactory.empty())
      .entrySet()
      .asScala
      .collect {
        case e if e.getKey.endsWith("port") =>
          "microservice.services." + e.getKey -> stubPort
      }
      .toMap

  }

  def extraConfig: Map[String, Any] = Map[String, Any](
    "run.mode"           -> "Test",
    "application.router" -> "testOnlyDoNotUseInAppConf.Routes"
  )

  val stubHost = "localhost"
  val stubPort: Int = sys.env.getOrElse("WIREMOCK_SERVICE_LOCATOR_PORT", "6008").toInt
  val wireMockServer = new WireMockServer(wireMockConfig().port(stubPort))

  override def beforeAll(): Unit = {
    super.beforeAll()
    wireMockServer.start()
    WireMock.configureFor(stubHost, stubPort)
    stubFor(post(urlPathMatching("/registration")).willReturn(aResponse().withStatus(204)))
  }

  override def afterAll(): Unit = {
    super.afterAll()
    wireMockServer.stop()
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    wireMockServer.resetMappings()
  }
}
