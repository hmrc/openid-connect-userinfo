package it

class UserInfoServiceSpec extends BaseFeatureSpec {

  feature("fetch user information") {

    scenario("fetch user profile") {

      Given("A token with 'openid' and 'profile' scopes")

      And("The token has a NINO")

      And("DES contains user information for the NINO")

      When("We request the user information")

      Then("The user information is returned")
    }

    scenario("fetch user address") {

      Given("A token with 'openid' and 'address' scopes")

      And("The token has a NINO")

      And("DES contains user information for the NINO")

      When("We request the user information")

      Then("The user address is returned")
    }

    scenario("fetch user identifiers") {

      Given("A token with 'openid' and 'openid:uk-gov-identifiers' scopes")

      And("The token has a NINO")

      And("DES contains user information for the NINO")

      When("We request the user information")

      Then("The nino is returned")
    }
  }
}
