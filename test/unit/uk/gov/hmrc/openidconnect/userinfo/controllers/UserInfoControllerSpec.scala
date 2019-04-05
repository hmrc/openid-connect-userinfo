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

package unit.uk.gov.hmrc.openidconnect.userinfo.controllers

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.joda.time.LocalDate
import org.mockito.BDDMockito.given
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier}
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier}
import uk.gov.hmrc.openidconnect.userinfo.config.AppContext
import uk.gov.hmrc.openidconnect.userinfo.controllers.{ErrorBadRequest, LiveUserInfoController, SandboxUserInfoController}
import uk.gov.hmrc.openidconnect.userinfo.domain.{Address, GovernmentGatewayDetails, UserInfo}
import uk.gov.hmrc.openidconnect.userinfo.services.{LiveUserInfoService, SandboxUserInfoService}
import uk.gov.hmrc.play.test.{UnitSpec}

import scala.concurrent.Future

class UserInfoControllerSpec extends UnitSpec with MockitoSugar with ScalaFutures {

  implicit val actorSystem: ActorSystem = ActorSystem("test")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val userInfo = UserInfo(
    Some("John"),
    Some("Smith"),
    Some("Hannibal"),
    Some(Address("221B\\BAKER STREET\\nLONDON\\NW1 9NT\\nUnited Kingdom", Some("NW1 9NT"), Some("United Kingdom"), Some("GB"))),
    Some("John.Smith@a.b.c.com"),
    Some(LocalDate.parse("1982-11-15")),
    Some("AR778351B"),
    Some(Set(Enrolment("IR-SA", List(EnrolmentIdentifier("UTR", "174371121")), "Activated"))),
    Some(GovernmentGatewayDetails(Some("32131"),Some(Seq("User")), Some("John"), Some("affinityGroup"), Some("agent-code-12345"),
      Some("agent-id-12345"), Some("agent-friendly-name"), None, None)), None)

  trait Setup {
    val mockAppContext = mock[AppContext]
    val mockLiveUserInfoService = mock[LiveUserInfoService]
    val mockSandboxUserInfoService = mock[SandboxUserInfoService]

    val sandboxController = new SandboxUserInfoController(mockSandboxUserInfoService, mockAppContext)
    val liveController = new LiveUserInfoController(mockLiveUserInfoService, mockAppContext)
  }

  "sandbox userInfo" should {
    "retrieve user information" in new Setup {

      given(mockSandboxUserInfoService.fetchUserInfo()(Matchers.any[HeaderCarrier])).willReturn(userInfo)

      val result = await(sandboxController.userInfo()(FakeRequest().withHeaders("Accept" -> "application/vnd.hmrc.1.0+json")))

      status(result) shouldBe 200
      jsonBodyOf(result) shouldBe Json.toJson(userInfo)
    }

    "fail with 406 (Not Acceptable) if version headers not present" in new Setup {

      given(mockSandboxUserInfoService.fetchUserInfo()(Matchers.any[HeaderCarrier])).willReturn(userInfo)

      val result = await(sandboxController.userInfo()(FakeRequest()))

      status(result) shouldBe 406
    }
  }

  "live userInfo" should {

    "retrieve user information" in new Setup {

      given(mockLiveUserInfoService.fetchUserInfo()(any[HeaderCarrier])).willReturn(userInfo)

      val result = await(liveController.userInfo()(FakeRequest().withHeaders("Accept" -> "application/vnd.hmrc.1.0+json")))

      status(result) shouldBe 200
      jsonBodyOf(result) shouldBe Json.toJson(userInfo)
    }

    "fail with 406 (Not Acceptable) if version headers not present" in new Setup {

      val result = await(liveController.userInfo()(FakeRequest()))

      status(result) shouldBe 406
    }

    "fail with Bad Request if service throws BadRequestException" in new Setup {

      given(mockLiveUserInfoService.fetchUserInfo()(any[HeaderCarrier])).willReturn(Future.failed(new BadRequestException("NINO is required")))

      val result = await(liveController.userInfo()(FakeRequest().withHeaders("Accept" -> "application/vnd.hmrc.1.0+json")))

      status(result) shouldBe 400
      jsonBodyOf(result) shouldBe Json.toJson(ErrorBadRequest("NINO is required"))
    }
  }
}
