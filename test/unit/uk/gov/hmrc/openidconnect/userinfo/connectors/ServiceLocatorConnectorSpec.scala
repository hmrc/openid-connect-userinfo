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

package uk.gov.hmrc.openidconnect.userinfo.unit.connectors

import uk.gov.hmrc.openidconnect.userinfo.connectors.ServiceLocatorConnector
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.api.domain.Registration
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpPost, HttpResponse}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class ServiceLocatorConnectorSpec extends UnitSpec with MockitoSugar with ScalaFutures {

  trait Setup {
    implicit val hc = HeaderCarrier()
    val serviceLocatorException = new RuntimeException

    val connector = new ServiceLocatorConnector {
      override val http = mock[HttpPost]
      override val appUrl: String = "http://openid-connect-userinfo.service"
      override val appName: String = "openid-connect-userinfo"
      override val serviceUrl: String = "https://SERVICE_LOCATOR"
      override val handlerOK: () => Unit = mock[Function0[Unit]]
      override val handlerError: Throwable => Unit = mock[Function1[Throwable, Unit]]
      override val metadata: Option[Map[String, String]] = Some(Map("third-party-api" -> "true"))
    }
  }

  "register" should {
    "register the JSON API Definition into the Service Locator" in new Setup {

      val registration = Registration(serviceName = "openid-connect-userinfo", serviceUrl = "http://openid-connect-userinfo.service", metadata = Some(Map("third-party-api" -> "true")))

      when(connector.http.POST(s"${connector.serviceUrl}/registration", registration, Seq("Content-Type"-> "application/json"))).thenReturn(Future.successful(HttpResponse(200)))

      connector.register.futureValue shouldBe true
      verify(connector.http).POST("https://SERVICE_LOCATOR/registration", registration, Seq("Content-Type"-> "application/json"))
      verify(connector.handlerOK).apply()
      verify(connector.handlerError, never).apply(serviceLocatorException)
    }


    "fail registering in service locator" in new Setup {

      val registration = Registration(serviceName = "openid-connect-userinfo", serviceUrl = "http://openid-connect-userinfo.service", metadata = Some(Map("third-party-api" -> "true")))
      when(connector.http.POST(s"${connector.serviceUrl}/registration", registration, Seq("Content-Type"-> "application/json"))).thenReturn(Future.failed(serviceLocatorException))

      connector.register.futureValue shouldBe false
      verify(connector.http).POST("https://SERVICE_LOCATOR/registration", registration, Seq("Content-Type"-> "application/json"))
      verify(connector.handlerOK, never).apply()
      verify(connector.handlerError).apply(serviceLocatorException)
    }

  }
}

