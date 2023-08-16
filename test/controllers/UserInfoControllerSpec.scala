/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers

import akka.actor.ActorSystem
import config.AppContext
import domain.{Address, GovernmentGatewayDetails, UserInfo}
import java.time.LocalDate
import org.mockito.BDDMockito.given
import org.scalatest.concurrent.ScalaFutures
import org.mockito.scalatest.MockitoSugar
import play.api.libs.json.Json
import play.api.mvc.ControllerComponents
import play.api.test.FakeRequest
import services.{LiveUserInfoService, SandboxUserInfoService}
import testSupport.UnitSpec
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier}
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier}

import scala.concurrent.{ExecutionContext, Future}

class UserInfoControllerSpec(implicit val cc: ControllerComponents, ex: ExecutionContext) extends UnitSpec with MockitoSugar with ScalaFutures {

  implicit val actorSystem: ActorSystem = ActorSystem("test")
  val ACCEPT_HEADER_V1_0 = "application/vnd.hmrc.1.0+json"
  val ACCEPT_HEADER_V1_1 = "application/vnd.hmrc.1.1+json"

  val ggDetailsV1 = GovernmentGatewayDetails(
    Some("32131"),
    Some(Seq("User")),
    Some("John"),
    Some("affinityGroup"),
    Some("agent-code-12345"),
    Some("agent-id-12345"),
    Some("agent-friendly-name"),
    None,
    None,
    None,
    None
  )
  val ggDetailsV11 = ggDetailsV1.copy(profile_uri = Some("some_url"), group_profile_uri = Some("some_other_url"))

  val userInfoV1 = UserInfo(
    Some("John"),
    Some("Smith"),
    Some("Hannibal"),
    Some(Address("221B\\BAKER STREET\\nLONDON\\NW1 9NT\\nUnited Kingdom", Some("NW1 9NT"), Some("United Kingdom"), Some("GB"))),
    Some("John.Smith@a.b.c.com"),
    Some(LocalDate.parse("1982-11-15")),
    Some("AR778351B"),
    Some(Set(Enrolment("IR-SA", List(EnrolmentIdentifier("UTR", "174371121")), "Activated"))),
    Some(ggDetailsV1),
    None
  )
  val userInfoV11 = userInfoV1.copy(government_gateway = Some(ggDetailsV11))

  trait Setup {
    val mockAppContext = mock[AppContext]
    val mockLiveUserInfoService = mock[LiveUserInfoService]
    val mockSandboxUserInfoService = mock[SandboxUserInfoService]

    val sandboxController = new SandboxUserInfoController(mockSandboxUserInfoService, mockAppContext, cc)
    val liveController = new LiveUserInfoController(mockLiveUserInfoService, mockAppContext, cc)
  }

  "sandbox userInfo" should {
    "retrieve user information v1.0" in new Setup {

      given(mockSandboxUserInfoService.fetchUserInfo(eqTo(Version_1_0))(any[HeaderCarrier])).willReturn(userInfoV1)

      val result = await(sandboxController.userInfo()(FakeRequest().withHeaders("Accept" -> "application/vnd.hmrc.1.0+json")))

      status(result)     shouldBe 200
      jsonBodyOf(result) shouldBe Json.toJson(userInfoV1)
    }

    "fail with 406 (Not Acceptable) if version headers not present" in new Setup {

      given(mockSandboxUserInfoService.fetchUserInfo(eqTo(Version_1_0))(any[HeaderCarrier])).willReturn(userInfoV1)

      val result = await(sandboxController.userInfo()(FakeRequest()))

      status(result) shouldBe 406
    }
  }

  "live userInfo" should {

    "retrieve user information v1.0" in new Setup {

      given(mockLiveUserInfoService.fetchUserInfo(eqTo(Version_1_0))(any[HeaderCarrier])).willReturn(userInfoV1)

      val result = await(liveController.userInfo()(FakeRequest().withHeaders("Accept" -> "application/vnd.hmrc.1.0+json")))

      status(result)     shouldBe 200
      jsonBodyOf(result) shouldBe Json.toJson(userInfoV1)
    }

    "fail with 406 (Not Acceptable) if version headers not present" in new Setup {

      val result = await(liveController.userInfo()(FakeRequest()))

      status(result) shouldBe 406
    }

    "fail with Bad Request if service throws BadRequestException" in new Setup {

      given(mockLiveUserInfoService.fetchUserInfo(eqTo(Version_1_0))(any[HeaderCarrier]))
        .willReturn(Future.failed(new BadRequestException("NINO is required")))

      val result = await(liveController.userInfo()(FakeRequest().withHeaders("Accept" -> "application/vnd.hmrc.1.0+json")))

      status(result)     shouldBe 400
      jsonBodyOf(result) shouldBe Json.toJson(ErrorBadRequest("NINO is required"))
    }
  }
}
