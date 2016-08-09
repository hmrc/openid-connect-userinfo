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

package uk.gov.hmrc.openidconnect.userinfo.services

import uk.gov.hmrc.openidconnect.userinfo.data.UserInfoGenerator
import uk.gov.hmrc.openidconnect.userinfo.domain.{Address, UserInfo}
import org.joda.time.LocalDate
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.play.http.{HeaderCarrier, NotImplementedException}
import uk.gov.hmrc.play.test.UnitSpec

class UserInfoServiceSpec extends UnitSpec with MockitoSugar with ScalaFutures {

  trait Setup {
    implicit val headers = HeaderCarrier()

    val liveInfoService = LiveUserInfoService
    val mockUserInfoGenerator = mock[UserInfoGenerator]

    val sandboxInfoService = new SandboxUserInfoService {
      override val userInfoGenerator = mockUserInfoGenerator
    }

    val userInfo = UserInfo(
      "John",
      "Smith",
      Some("Hannibal"),
      Address("221B BAKER STREET\nLONDON NW1 9NT\nUnited Kingdom", "NW1 9NT", "United Kingdom"),
      new LocalDate(1982,11, 15),
      "AR778351B")

    when(mockUserInfoGenerator.userInfo).thenReturn(userInfo)
  }

  "LiveUserInfoService" should {
    "throw NotImplementedException when attempting to retrieve user information" in new Setup {
      intercept[NotImplementedException](await(liveInfoService.fetchUserInfo()))
    }
  }

  "SandboxUserInfoService" should {
    "return generated UserInfo" in new Setup {
      val result = await(sandboxInfoService.fetchUserInfo())
      result shouldBe userInfo
    }
  }

}