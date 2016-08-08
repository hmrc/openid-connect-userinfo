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

package controllers

import data.UserInfoGenerator
import domain.{Address, UserInfo}
import org.joda.time.DateTime
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.Json
import play.api.test.FakeRequest
import services.SandboxUserInfoService
import uk.gov.hmrc.play.test.UnitSpec

class UserInfoControllerSpec extends UnitSpec with MockitoSugar with ScalaFutures {

  trait Setup {
    val mockUserInfoGenerator = mock[UserInfoGenerator]

    object TestSandboxInfoService extends SandboxUserInfoService {
      override val userInfoGenerator = mockUserInfoGenerator
    }

    val sandboxController = new SandboxUserInfoController {
      override val service = TestSandboxInfoService
    }

    val liveController = LiveUserInfoController

    val userInfo = UserInfo(
      "John",
      "Smith",
      Some("Hannibal"),
      Address("221B\\BAKER STREET\\nLONDON\\NW1 9NT\\nUnited Kingdom", "NW1 9NT", "United Kingdom"),
      new DateTime("1982-11-15").toLocalDate,
      "AR778351B")

    when(mockUserInfoGenerator.userInfo).thenReturn(userInfo)
  }

  "sandbox userInfo" should {
    "retrieve user information" in new Setup {
      val result = await(sandboxController.userInfo()(FakeRequest().withHeaders("Accept" -> "application/vnd.hmrc.1.0+json")))

      status(result) shouldBe 200
      jsonBodyOf(result) shouldBe Json.toJson(userInfo)
    }

    "fail with 406 (Not Acceptable) if version headers not present" in new Setup {
      val result = await(sandboxController.userInfo()(FakeRequest()))
      status(result) shouldBe 406
    }
  }

  "live userInfo" should {
    "fail with 501 (Not Implemented)" in new Setup {
      val result = await(liveController.userInfo()(FakeRequest().withHeaders("Accept" -> "application/vnd.hmrc.1.0+json")))
      status(result) shouldBe 501
      bodyOf(result) shouldBe """{"code":"NOT_IMPLEMENTED","message":"Live service endpoint is not implemented"}"""
    }

    "fail with 406 (Not Acceptable) if version headers not present" in new Setup {
      val result = await(sandboxController.userInfo()(FakeRequest()))
      status(result) shouldBe 406
    }

  }
}
