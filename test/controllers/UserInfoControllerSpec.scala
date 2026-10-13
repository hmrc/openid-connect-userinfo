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

package controllers

import ch.qos.logback.classic.Level
import config.AppContext
import domain.{Address, GovernmentGatewayDetails, UserInfo}
import org.apache.pekko.actor.ActorSystem
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.ControllerComponents
import play.api.test.FakeRequest
import play.api.test.Helpers.stubControllerComponents
import services.{LiveUserInfoService, SandboxUserInfoService}
import testSupport.UnitSpec
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier}
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.tools.LogCapturing

import java.time.LocalDate
import scala.concurrent.duration.*
import scala.concurrent.{Await, ExecutionContext, Future}

class UserInfoControllerSpec extends UnitSpec with MockitoSugar with ScalaFutures with LogCapturing with BeforeAndAfterAll {

  implicit val actorSystem: ActorSystem = ActorSystem("test")
  implicit val ec: ExecutionContext = actorSystem.dispatcher
  val cc: ControllerComponents = stubControllerComponents()

  override def afterAll(): Unit =
    try Await.result(actorSystem.terminate(), 10.seconds)
    finally super.afterAll()

  private val liveControllerLogger = Logger(classOf[LiveUserInfoController])
  private val responsePayloadLogger = Logger("userInfoResponsePayloadLogger")
  private val acceptHeader = "Accept" -> "application/vnd.hmrc.1.0+json"

  val ggDetailsV1: GovernmentGatewayDetails = GovernmentGatewayDetails(
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
  val ggDetailsV11: GovernmentGatewayDetails = ggDetailsV1.copy(profile_uri = Some("some_url"), group_profile_uri = Some("some_other_url"))

  val userInfoV1: UserInfo = UserInfo(
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
  val userInfoV11: UserInfo = userInfoV1.copy(government_gateway = Some(ggDetailsV11))

  trait Setup {
    val mockAppContext: AppContext = mock[AppContext]
    val mockLiveUserInfoService: LiveUserInfoService = mock[LiveUserInfoService]
    val mockSandboxUserInfoService: SandboxUserInfoService = mock[SandboxUserInfoService]

    val sandboxController = new SandboxUserInfoController(mockSandboxUserInfoService, mockAppContext, cc)
    val liveController = new LiveUserInfoController(mockLiveUserInfoService, mockAppContext, cc)
  }

  "sandbox userInfo" should {
    "retrieve user information v1.0" in new Setup {

      when(mockSandboxUserInfoService.fetchUserInfo(eqTo(Version_1_0))(any[HeaderCarrier])).thenReturn(Future.successful(userInfoV1))

      val result = await(sandboxController.userInfo()(FakeRequest().withHeaders("Accept" -> "application/vnd.hmrc.1.0+json")))

      status(result)     shouldBe 200
      jsonBodyOf(result) shouldBe Json.toJson(userInfoV1)
    }

    "fail with 406 (Not Acceptable) if an unsupported version is requested" in new Setup {

      when(mockSandboxUserInfoService.fetchUserInfo(eqTo(Version_1_0))(any[HeaderCarrier])).thenReturn(Future.successful(userInfoV1))

      val result = await(sandboxController.userInfo()(FakeRequest().withHeaders("Accept" -> "application/vnd.hmrc.2.0+json")))

      status(result) shouldBe 406
    }
  }

  "live userInfo" should {

    "retrieve user information v1.0" in new Setup {

      when(mockLiveUserInfoService.fetchUserInfo(eqTo(Version_1_0))(any[HeaderCarrier])).thenReturn(Future.successful(userInfoV1))

      val result = await(liveController.userInfo()(FakeRequest().withHeaders("Accept" -> "application/vnd.hmrc.1.0+json")))

      status(result)     shouldBe 200
      jsonBodyOf(result) shouldBe Json.toJson(userInfoV1)
    }

    "fail with 406 (Not Acceptable) if an unsupported version is requested" in new Setup {

      val result = await(liveController.userInfo()(FakeRequest().withHeaders("Accept" -> "application/vnd.hmrc.2.0+json")))

      status(result) shouldBe 406
    }

    "fail with Bad Request if service throws BadRequestException" in new Setup {

      when(mockLiveUserInfoService.fetchUserInfo(eqTo(Version_1_0))(any[HeaderCarrier]))
        .thenReturn(Future.failed(new BadRequestException("NINO is required")))

      val result = await(liveController.userInfo()(FakeRequest().withHeaders("Accept" -> "application/vnd.hmrc.1.0+json")))

      status(result)     shouldBe 400
      jsonBodyOf(result) shouldBe Json.toJson(ErrorResponse.badRequest("NINO is required"))
    }

    "fail with Bad Gateway without exposing upstream error detail if service returns an upstream 5xx" in new Setup {
      val upstreamMessage = "POST of 'http://localhost:1234/auth/authorise' returned 500. Response body: 'auth error detail'"

      when(mockLiveUserInfoService.fetchUserInfo(eqTo(Version_1_0))(any[HeaderCarrier]))
        .thenReturn(Future.failed(UpstreamErrorResponse(upstreamMessage, 500)))

      val result = await(liveController.userInfo()(FakeRequest().withHeaders("Accept" -> "application/vnd.hmrc.1.0+json")))

      status(result)     shouldBe 502
      jsonBodyOf(result) shouldBe Json.toJson(ErrorResponse.badGateway())
      bodyOf(result)       should not include "auth error detail"
    }

    "fail with Bad Gateway without exposing upstream error detail if service returns an upstream 4xx" in new Setup {
      val upstreamMessage = "POST of 'http://localhost:1234/auth/authorise' returned 403. Response body: 'auth error detail'"

      when(mockLiveUserInfoService.fetchUserInfo(eqTo(Version_1_0))(any[HeaderCarrier]))
        .thenReturn(Future.failed(UpstreamErrorResponse(upstreamMessage, 403)))

      val result = await(liveController.userInfo()(FakeRequest().withHeaders("Accept" -> "application/vnd.hmrc.1.0+json")))

      status(result)     shouldBe 502
      jsonBodyOf(result) shouldBe Json.toJson(ErrorResponse.badGateway())
      bodyOf(result)       should not include "auth error detail"
    }

    "fail with Unauthorized if service returns an upstream 401" in new Setup {
      when(mockLiveUserInfoService.fetchUserInfo(eqTo(Version_1_0))(any[HeaderCarrier]))
        .thenReturn(Future.failed(UpstreamErrorResponse("auth error detail", 401)))

      val result = await(liveController.userInfo()(FakeRequest().withHeaders("Accept" -> "application/vnd.hmrc.1.0+json")))

      status(result)     shouldBe 401
      jsonBodyOf(result) shouldBe Json.toJson(ErrorResponse.unauthorized())
    }
  }

  "live userInfo logging" should {

    "log an upstream 4xx at WARN with the upstream detail" in new Setup {
      when(mockLiveUserInfoService.fetchUserInfo(eqTo(Version_1_0))(any[HeaderCarrier]))
        .thenReturn(Future.failed(UpstreamErrorResponse("POST of 'http://auth/authorise' returned 403", 403)))

      withCaptureOfLoggingFrom(liveControllerLogger) { logs =>
        val result = await(liveController.userInfo()(FakeRequest().withHeaders(acceptHeader)))

        status(result)       shouldBe 502
        logs.map(_.getLevel) shouldBe List(Level.WARN)
        logs.head.getMessage   should include("returned 403")
      }
    }

    "log an upstream 5xx at ERROR with the upstream detail" in new Setup {
      when(mockLiveUserInfoService.fetchUserInfo(eqTo(Version_1_0))(any[HeaderCarrier]))
        .thenReturn(Future.failed(UpstreamErrorResponse("POST of 'http://auth/authorise' returned 503", 503)))

      withCaptureOfLoggingFrom(liveControllerLogger) { logs =>
        val result = await(liveController.userInfo()(FakeRequest().withHeaders(acceptHeader)))

        status(result)       shouldBe 502
        logs.map(_.getLevel) shouldBe List(Level.ERROR)
        logs.head.getMessage   should include("returned 503")
      }
    }

    "not log an upstream 401" in new Setup {
      when(mockLiveUserInfoService.fetchUserInfo(eqTo(Version_1_0))(any[HeaderCarrier]))
        .thenReturn(Future.failed(UpstreamErrorResponse("auth error detail", 401)))

      withCaptureOfLoggingFrom(liveControllerLogger) { logs =>
        val result = await(liveController.userInfo()(FakeRequest().withHeaders(acceptHeader)))

        status(result) shouldBe 401
        logs           shouldBe empty
      }
    }

    "not log the response payload when payload logging is disabled" in new Setup {
      when(mockAppContext.logUserInfoResponsePayload).thenReturn(false)
      when(mockLiveUserInfoService.fetchUserInfo(eqTo(Version_1_0))(any[HeaderCarrier])).thenReturn(Future.successful(userInfoV1))

      withCaptureOfLoggingFrom(responsePayloadLogger) { logs =>
        val result = await(liveController.userInfo()(FakeRequest().withHeaders(acceptHeader)))

        status(result) shouldBe 200
        logs           shouldBe empty
      }
    }

    "log the response payload at DEBUG when payload logging is enabled" in new Setup {
      when(mockAppContext.logUserInfoResponsePayload).thenReturn(true)
      when(mockLiveUserInfoService.fetchUserInfo(eqTo(Version_1_0))(any[HeaderCarrier])).thenReturn(Future.successful(userInfoV1))

      withCaptureOfLoggingFrom(responsePayloadLogger) { logs =>
        val result = await(liveController.userInfo()(FakeRequest().withHeaders(acceptHeader)))

        status(result)       shouldBe 200
        logs.map(_.getLevel) shouldBe List(Level.DEBUG)
        logs.head.getMessage   should include("AR778351B")
      }
    }
  }
}
