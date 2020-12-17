import play.api.libs.json.Json
import scalaj.http.Http

class FeatureSwitchControllerISpec extends BaseFeatureISpec {

  val serviceUrl = "/feature-switches"

  feature("getting feature switches") {
    scenario("calling GET /feature-switches") {
      Given("we call GET /feature-switches")

      val result = Http(resource(s"$serviceUrl")).asString

      Then("the feature switches are returned as json with 200 OK")

      result.code shouldBe 200
      Json.parse(result.body) shouldBe Json.arr(Json.obj("name" -> "countryCode", "isEnabled" -> false), Json.obj("name" -> "addressLine5", "isEnabled" -> false))
    }
  }

  feature("updating feature switches") {
    scenario("calling POST /feature-switches") {
      Given("we call GET /feature-switches")

      val result = Http(resource(s"$serviceUrl")).asString

      Then("the feature switches are returned as json with 200 OK")

      result.code shouldBe 200
      Json.parse(result.body) shouldBe Json.arr(Json.obj("name" -> "countryCode", "isEnabled" -> false), Json.obj("name" -> "addressLine5", "isEnabled" -> false))

      When("we update the flags we should get 406 Accepted")

      val payload = Json.obj("featureSwitches" -> Json.arr(Json.obj("name" -> "countryCode", "isEnabled" -> true), Json.obj("name" -> "addressLine5", "isEnabled" -> true)))

      val updateResult = Http(resource(s"$serviceUrl")).method("POST").header("Content-Type", "application/json").postData(payload.toString()).asString

      updateResult.code shouldBe 202

      When("we retrieve the flags back we see they should be negated")

      val updatedResult = Http(resource(s"$serviceUrl")).asString

      updatedResult.code shouldBe 200
      Json.parse(updatedResult.body) shouldBe Json.arr(Json.obj("name" -> "countryCode", "isEnabled" -> true), Json.obj("name" -> "addressLine5", "isEnabled" -> true))

    }
  }
}
