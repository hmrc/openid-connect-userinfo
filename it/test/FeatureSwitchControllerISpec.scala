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

import play.api.libs.json.Json
import scalaj.http.Http

class FeatureSwitchControllerISpec extends BaseFeatureISpec {

  val serviceUrl = "/test-only/feature-switches"

  Feature("getting feature switches") {
    Scenario("calling GET /test-only/feature-switches") {
      Given("we call GET /test-only/feature-switches")

      val result = Http(resource(s"$serviceUrl")).asString

      Then("the feature switches are returned as json with 200 OK")

      result.code shouldBe 200
      Json.parse(result.body) shouldBe Json.arr(Json.obj("name" -> "countryCode", "isEnabled" -> false),
                                                Json.obj("name" -> "addressLine5", "isEnabled" -> false)
                                               )
    }
  }

  Feature("updating feature switches") {
    Scenario("calling POST /test-only/feature-switches") {
      Given("we call GET /test-only/feature-switches")

      val result = Http(resource(s"$serviceUrl")).asString

      Then("the feature switches are returned as json with 200 OK")

      result.code shouldBe 200
      Json.parse(result.body) shouldBe Json.arr(Json.obj("name" -> "countryCode", "isEnabled" -> false),
                                                Json.obj("name" -> "addressLine5", "isEnabled" -> false)
                                               )

      When("we update the flags we should get 406 Accepted")

      val payload = Json.obj(
        "featureSwitches" -> Json.arr(Json.obj("name" -> "countryCode", "isEnabled" -> true), Json.obj("name" -> "addressLine5", "isEnabled" -> true))
      )

      val updateResult =
        Http(resource(s"$serviceUrl")).method("POST").header("Content-Type", "application/json").postData(payload.toString()).asString

      updateResult.code shouldBe 202

      When("we retrieve the flags back we see they should be negated")

      val updatedResult = Http(resource(s"$serviceUrl")).asString

      updatedResult.code shouldBe 200
      Json.parse(updatedResult.body) shouldBe Json.arr(Json.obj("name" -> "countryCode", "isEnabled" -> true),
                                                       Json.obj("name" -> "addressLine5", "isEnabled" -> true)
                                                      )

    }
  }
}
