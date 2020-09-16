/*
 * Copyright 2020 HM Revenue & Customs
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

package unit.uk.gov.hmrc.openidconnect.userinfo.controllers.testOnly

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.scalatest.concurrent.ScalaFutures
import play.api.libs.json._
import play.api.mvc.{ControllerComponents, Request, Result}
import play.api.test.FakeRequest
import uk.gov.hmrc.openidconnect.userinfo.config.{FeatureSwitch, UserInfoFeatureSwitches}
import uk.gov.hmrc.openidconnect.userinfo.controllers.testOnly.FeatureSwitchController
import unit.uk.gov.hmrc.openidconnect.UnitSpec

import scala.concurrent.ExecutionContext

class FeatureSwitchControllerSpec(implicit val cc: ControllerComponents, val ex: ExecutionContext) extends UnitSpec with ScalaFutures {

  implicit val actorSystem: ActorSystem = ActorSystem("test")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  trait Setup {
    def featuresList(countryCodeEnabled: Boolean = false, addressLine5Enabled: Boolean = false): JsValue = Json.arr(
      Json.obj("name" -> "countryCode", "isEnabled" -> countryCodeEnabled),
      Json.obj("name" -> "addressLine5", "isEnabled" -> addressLine5Enabled)
    )

    val controller = new FeatureSwitchController
    UserInfoFeatureSwitches.allSwitches.map(FeatureSwitch.disable(_))
  }

  "FeatureSwitchController" should {
    "return a list of switches" in new Setup {
      val expectedBody = featuresList()

      val result = await(controller.getFlags()(FakeRequest()))
      status(result) shouldBe 200
      jsonBodyOf(result) shouldBe expectedBody
    }

    "enable the country code" in new Setup {
      val expectedBody = featuresList(countryCodeEnabled = true)

      val changeRequest = Json.obj("featureSwitches" -> Json.arr(Json.obj("name" -> "countryCode", "isEnabled" -> true)))
      val updateRequest: Request[JsValue] = FakeRequest().withBody(changeRequest)

      val result: Result = await(controller.setFlags()(updateRequest))

      status(result) shouldBe 202
      jsonBodyOf(result) shouldBe expectedBody
    }

    "disable the address line 5" in new Setup {
      val expectedBody = featuresList()

      val changeRequest = Json.obj("featureSwitches" -> Json.arr(Json.obj("name" -> "addressLine5", "isEnabled" -> false)))
      val updateRequest: Request[JsValue] = FakeRequest().withBody(changeRequest)

      val result: Result = await(controller.setFlags()(updateRequest))

      status(result) shouldBe 202
      jsonBodyOf(result) shouldBe expectedBody
    }
  }
}
