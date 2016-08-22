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

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import org.mockito.Matchers._
import uk.gov.hmrc.openidconnect.userinfo.connectors.AuthConnector
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

class AuthServiceSpec extends UnitSpec with ScalaFutures with MockitoSugar {

  trait Setup {
    implicit val hc = HeaderCarrier()

    val authService = new AuthService {
      override val authConnector = mock[AuthConnector]
    }
  }

  "isAuthorised" should {
    "return true if confidenceLevel is 200" in new Setup {
      when(authService.authConnector.confidenceLevel()(any())).thenReturn(Some(200))

      val result = authService.isAuthorised()

      result.futureValue shouldBe true
    }

    "return true if confidenceLevel is above 200" in new Setup {
      when(authService.authConnector.confidenceLevel()(any())).thenReturn(Some(300))

      val result = authService.isAuthorised()

      result.futureValue shouldBe true
    }

    "return false if confidenceLevel is below 200" in new Setup {
      when(authService.authConnector.confidenceLevel()(any())).thenReturn(Some(50))

      val result = authService.isAuthorised()

      result.futureValue shouldBe false
    }

    "return false if no confidenceLevel returned from auth" in new Setup {
      when(authService.authConnector.confidenceLevel()(any())).thenReturn(None)

      val result = authService.isAuthorised()

      result.futureValue shouldBe false
    }
  }
}
