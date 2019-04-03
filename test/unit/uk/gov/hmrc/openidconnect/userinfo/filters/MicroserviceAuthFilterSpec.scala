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

package unit.uk.gov.hmrc.openidconnect.userinfo.filters

import org.mockito.Matchers
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.Json.obj
import play.api.mvc.RequestHeader
import play.api.mvc.Results._
import play.api.routing.Router
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.authorise.EmptyPredicate
import uk.gov.hmrc.auth.core.retrieve.EmptyRetrieval
import uk.gov.hmrc.auth.core.{AuthConnector, MissingBearerToken}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.openidconnect.userinfo.filters.MicroserviceAuthFilter
import uk.gov.hmrc.play.auth.controllers.{AuthConfig, AuthParamsControllerConfig}
import uk.gov.hmrc.play.auth.microservice.connectors.ConfidenceLevel
import uk.gov.hmrc.play.microservice.filters.MicroserviceFilterSupport
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.Future

class MicroserviceAuthFilterSpec extends UnitSpec with ScalaFutures with MockitoSugar with WithFakeApplication {

  val sandbox = "sandbox"
  val live = "live"

  trait Setup extends MicroserviceFilterSupport {

    val authFilter = new MicroserviceAuthFilter with MicroserviceFilterSupport {
      override val authParamsConfig = mock[AuthParamsControllerConfig]
      override val authConnector = mock[AuthConnector]

      override def controllerNeedsAuth(controllerName: String): Boolean = {
         controllerName match {
           case `sandbox` => false
           case `live` => true
           case _ => false
         }
      }
    }
  }

  "microserviceAuthFilter" should {

    implicit val hc = HeaderCarrier()

    def nextCall(rh: RequestHeader) = Future.successful(Ok("Success"))

    "call the next filter if authentication is required and the user is authorised" in new Setup {

      when(authFilter.authConnector.authorise(Matchers.eq(EmptyPredicate), Matchers.eq(EmptyRetrieval))(any(), any())).thenReturn(Future.successful())
      when(authFilter.authParamsConfig.authConfig(live)).thenReturn(AuthConfig(confidenceLevel = ConfidenceLevel.L50))

      val request = FakeRequest("GET", "/").copy(tags = Map(Router.Tags.RouteController -> live))
      val response = authFilter(nextCall _)(request).futureValue

      response.header.status shouldBe 200
      bodyOf(response) shouldBe "Success"
    }

    "return 401 (Unauthorized) if authentication is required and the user is not authorised" in new Setup {

      when(authFilter.authConnector.authorise(Matchers.eq(EmptyPredicate), Matchers.eq(EmptyRetrieval))(any(), any())).thenReturn(Future.failed(MissingBearerToken()))
      when(authFilter.authParamsConfig.authConfig(live)).thenReturn(AuthConfig(confidenceLevel = ConfidenceLevel.L50))

      val request = FakeRequest("GET", "/").copy(tags = Map(Router.Tags.RouteController -> live))
      val response = await(authFilter(nextCall _)(request))

      response.header.status shouldBe 401
      jsonBodyOf(response) shouldBe obj("code" -> "UNAUTHORIZED", "message" -> "Bearer token is missing or not authorized")
    }

    "call the next filter if authentication is not required" in new Setup {

      val request = FakeRequest("GET", "/").copy(tags = Map(Router.Tags.RouteController -> sandbox))

      val response = authFilter(nextCall _)(request).futureValue

      response.header.status shouldBe 200
      bodyOf(response) shouldBe "Success"

      verifyZeroInteractions(authFilter.authConnector, authFilter.authParamsConfig)
    }
  }
}
