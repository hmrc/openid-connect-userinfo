/*
 * Copyright 2017 HM Revenue & Customs
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

package it

import org.joda.time.LocalDate
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import play.api.libs.json.Json
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.openidconnect.userinfo.config.{FeatureSwitch, UserInfoFeatureSwitches}
import uk.gov.hmrc.openidconnect.userinfo.domain._
import uk.gov.hmrc.play.auth.microservice.connectors.ConfidenceLevel
import uk.gov.hmrc.play.http.Token

import scalaj.http.Http

class UserInfoServiceSpec extends BaseFeatureSpec with BeforeAndAfterAll {

  override protected def beforeAll() = {
    FeatureSwitch.enable(UserInfoFeatureSwitches.countryCode)
    FeatureSwitch.enable(UserInfoFeatureSwitches.addressLine5)
  }
  override protected def afterAll() = {
    FeatureSwitch.disable(UserInfoFeatureSwitches.countryCode)
    FeatureSwitch.disable(UserInfoFeatureSwitches.addressLine5)
  }

  val authBearerToken = "AUTH_BEARER_TOKEN"
  val nino = "AB123456A"
  val ukCountryCode = 1
  val desUserInfo = DesUserInfo(DesUserName(Some("John"), Some("A"), Some("Smith")), Some(LocalDate.parse("1980-01-01")),
    DesAddress(Some("1 Station Road"), Some("Town Centre"), Some("London"), Some("England"), Some("UK"), Some("NW1 6XE"), Some(ukCountryCode)))
  val enrolments = Seq(Enrolment("IR-SA", List(EnrolmentIdentifier("UTR", "174371121"))))
  val government_gateway: GovernmentGatewayDetails = GovernmentGatewayDetails(Some("1304372065861347"),Some(Token("ggToken")),Some("Admin"),Some("Individual"))
  val email = "my-email@abc.uk"

  val userInfo = UserInfo(
    Some("John"),
    Some("Smith"),
    Some("A"),
    Some(Address("1 Station Road\nTown Centre\nLondon\nEngland\nUK\nNW1 6XE\nGREAT BRITAIN\nGB", Some("NW1 6XE"), Some("GREAT BRITAIN"), Some("GB"))),
    Some(email),
    Some(LocalDate.parse("1980-01-01")),
    Some("AB123456A"),
    Some(enrolments),
    Some(government_gateway))
  val desUserInfoWithoutFirstName = DesUserInfo(DesUserName(None, Some("A"), Some("Smith")), Some(LocalDate.parse("1980-01-01")),
    DesAddress(Some("1 Station Road"), Some("Town Centre"), Some("London"), Some("England"),  Some("UK"), Some("NW1 6XE"), Some(ukCountryCode)))
  val userInfoWithoutFirstName = UserInfo(
    None,
    Some("Smith"),
    Some("A"),
    Some(Address("1 Station Road\nTown Centre\nLondon\nEngland\nUK\nNW1 6XE\nGREAT BRITAIN\nGB", Some("NW1 6XE"), Some("GREAT BRITAIN"), Some("GB"))),
    Some(email),
    Some(LocalDate.parse("1980-01-01")),
    Some("AB123456A"),
    Some(enrolments),
    Some(government_gateway)
  )
  val desUserInfoWithoutFamilyName = DesUserInfo(DesUserName(Some("John"), Some("A"), None), Some(LocalDate.parse("1980-01-01")),
    DesAddress(Some("1 Station Road"), Some("Town Centre"), Some("London"), Some("England"),  Some("UK"), Some("NW1 6XE"), Some(ukCountryCode)))
  val userInfoWithoutFamilyName = UserInfo(
    Some("John"),
    None,
    Some("A"),
    Some(Address("1 Station Road\nTown Centre\nLondon\nEngland\nUK\nNW1 6XE\nGREAT BRITAIN\nGB", Some("NW1 6XE"), Some("GREAT BRITAIN"), Some("GB"))),
    Some(email),
    Some(LocalDate.parse("1980-01-01")),
    Some("AB123456A"),
    Some(enrolments),
    Some(government_gateway))
  val desUserInfoWithPartialAddress = DesUserInfo(DesUserName(Some("John"), Some("A"), Some("Smith")), Some(LocalDate.parse("1980-01-01")),
    DesAddress(Some("1 Station Road"), None, Some("Lancaster"), Some("England"), Some("UK"), Some("NW1 6XE"), Some(ukCountryCode)))
  val userInfoWithPartialAddress = UserInfo(
    Some("John"),
    Some("Smith"),
    Some("A"),
    Some(Address("1 Station Road\nLancaster\nEngland\nUK\nNW1 6XE\nGREAT BRITAIN\nGB", Some("NW1 6XE"), Some("GREAT BRITAIN"), Some("GB"))),
    Some(email),
    Some(LocalDate.parse("1980-01-01")),
    Some("AB123456A"),
    None,
    Some(government_gateway))

  feature("fetch user information") {

    scenario("fetch user profile") {

      Given("A Auth token with 'openid', 'profile', 'address', 'openid:gov-uk-identifiers', 'openid:hmrc_enrolments', " +
        "'email' and 'openid:government_gateway' scopes")
      thirdPartyDelegatedAuthorityStub.willReturnScopesForAuthBearerToken(authBearerToken,
        Set("openid", "profile", "address", "openid:gov-uk-identifiers", "openid:hmrc_enrolments", "openid:government_gateway", "email"))

      And("The Auth token has a confidence level above 200 and a NINO")
      authStub.willReturnAuthorityWith(ConfidenceLevel.L200, Nino(nino))

      And("The authority has enrolments")
      authStub.willReturnEnrolmentsWith()

      And("DES contains user information for the NINO")
      desStub.willReturnUserInformation(desUserInfo, nino)

      And("UserDeails for the user")
      userDetailsStub.willReturnUserDetailsWith(email)

      When("We request the user information")
      val result = Http(s"$serviceUrl")
        .headers(Seq("Authorization" -> s"Bearer $authBearerToken", "Accept" -> "application/vnd.hmrc.1.0+json", "token" -> "ggToken"))
        .asString

      Then("The user information is returned")
      result.code shouldBe 200
      Json.parse(result.body) shouldBe Json.toJson(userInfo)
    }

    scenario("fetch user profile without first name") {

      Given("A Auth token with 'openid', 'profile', 'address', 'openid:gov-uk-identifiers', 'openid:hmrc_enrolments', 'email' and 'openid:government_gateway' scopes")
      thirdPartyDelegatedAuthorityStub.willReturnScopesForAuthBearerToken(authBearerToken,
        Set("openid", "profile", "address", "openid:gov-uk-identifiers", "openid:hmrc_enrolments", "openid:government_gateway", "email"))

      And("The Auth token has a confidence level above 200 and a NINO")
      authStub.willReturnAuthorityWith(ConfidenceLevel.L200, Nino(nino))

      And("The authority has enrolments")
      authStub.willReturnEnrolmentsWith()

      And("DES contains user information for the NINO")
      desStub.willReturnUserInformation(desUserInfoWithoutFirstName, nino)

      And("UserDeails for the user")
      userDetailsStub.willReturnUserDetailsWith(email)

      When("We request the user information")
      val result = Http(s"$serviceUrl")
        .headers(Seq("Authorization" -> s"Bearer $authBearerToken", "Accept" -> "application/vnd.hmrc.1.0+json", "token" -> "ggToken"))
        .asString

      Then("The user information is returned")
      result.code shouldBe 200
      Json.parse(result.body) shouldBe Json.toJson(userInfoWithoutFirstName)
    }

    scenario("fetch user profile without family name") {

      Given("A Auth token with 'openid', 'profile', 'address', 'openid:gov-uk-identifiers' and 'openid:hmrc_enrolments' scopes")
      thirdPartyDelegatedAuthorityStub.willReturnScopesForAuthBearerToken(authBearerToken,
        Set("openid", "profile", "address", "openid:gov-uk-identifiers", "openid:hmrc_enrolments"))

      And("The Auth token has a confidence level above 200 and a NINO")
      authStub.willReturnAuthorityWith(ConfidenceLevel.L200, Nino(nino))

      And("The authority has enrolments")
      authStub.willReturnEnrolmentsWith()

      And("DES contains user information for the NINO")
      desStub.willReturnUserInformation(desUserInfoWithoutFamilyName, nino)

      When("We request the user information")
      val result = Http(s"$serviceUrl")
        .headers(Seq("Authorization" -> s"Bearer $authBearerToken", "Accept" -> "application/vnd.hmrc.1.0+json"))
        .asString

      Then("The user information is returned")
      result.code shouldBe 200
      Json.parse(result.body) shouldBe Json.toJson(userInfoWithoutFamilyName.copy(government_gateway = None, email = None))
    }

    scenario("fetch user profile with partial address") {

      Given("A Auth token with 'openid', 'profile', 'address', 'email', and 'openid:gov-uk-identifiers' scopes")
      thirdPartyDelegatedAuthorityStub.willReturnScopesForAuthBearerToken(authBearerToken,
        Set("openid", "profile", "address", "openid:gov-uk-identifiers", "email"))

      And("The Auth token has a confidence level above 200 and a NINO")
      authStub.willReturnAuthorityWith(ConfidenceLevel.L200, Nino(nino))

      And("DES contains user information for the NINO")
      desStub.willReturnUserInformation(desUserInfoWithPartialAddress, nino)

      And("UserDeails for the user")
      userDetailsStub.willReturnUserDetailsWith(email)

      When("We request the user information")
      val result = Http(s"$serviceUrl")
        .headers(Seq("Authorization" -> s"Bearer $authBearerToken", "Accept" -> "application/vnd.hmrc.1.0+json"))
        .asString

      Then("The user information is returned")
      result.code shouldBe 200
      Json.parse(result.body) shouldBe Json.toJson(userInfoWithPartialAddress.copy(government_gateway = None))
    }

    scenario("fetch user data without enrolments when there are no enrolments") {

      Given("A Auth token with 'openid', 'profile', 'address', 'openid:gov-uk-identifiers', 'email', 'openid:government_gateway' and 'openid:hmrc_enrolments' scopes")
      thirdPartyDelegatedAuthorityStub.willReturnScopesForAuthBearerToken(authBearerToken,
        Set("openid", "profile", "address", "openid:gov-uk-identifiers", "openid:hmrc_enrolments", "openid:government_gateway", "email"))

      And("The Auth token has a confidence level above 200 and a NINO")
      authStub.willReturnAuthorityWith(ConfidenceLevel.L200, Nino(nino))

      And("DES contains user information for the NINO")
      desStub.willReturnUserInformation(desUserInfo, nino)

      And("UserDeails for the user")
      userDetailsStub.willReturnUserDetailsWith(email)

      When("We request the user information")
      val result = Http(s"$serviceUrl")
        .headers(Seq("Authorization" -> s"Bearer $authBearerToken", "Accept" -> "application/vnd.hmrc.1.0+json", "token" -> "ggToken"))
        .asString

      Then("The user information is returned")
      result.code shouldBe 200
      Json.parse(result.body) shouldBe Json.toJson(userInfo.copy(hmrc_enrolments = None))
    }

    scenario("fetch user data without address and user details when there are no address and user details") {

      Given("A Auth token with 'openid', 'profile', 'address', 'openid:gov-uk-identifiers' and 'openid:hmrc_enrolments' scopes")
      thirdPartyDelegatedAuthorityStub.willReturnScopesForAuthBearerToken(authBearerToken,
        Set("openid", "profile", "address", "openid:gov-uk-identifiers", "openid:hmrc_enrolments"))

      And("The Auth token has a confidence level above 200 and a NINO")
      authStub.willReturnAuthorityWith(ConfidenceLevel.L200, Nino(nino))

      And("The authority has enrolments")
      authStub.willReturnEnrolmentsWith()

      When("We request the user information")
      val result = Http(s"$serviceUrl")
        .headers(Seq("Authorization" -> s"Bearer $authBearerToken", "Accept" -> "application/vnd.hmrc.1.0+json"))
        .asString

      Then("The user information is returned")
      result.code shouldBe 200
      val userWithNinoAndEnrolmentsOnly = userInfo.copy(given_name = None, family_name = None, middle_name = None, address = None, birthdate = None, government_gateway = None, email = None)
      Json.parse(result.body) shouldBe Json.toJson(userWithNinoAndEnrolmentsOnly)
    }

    scenario("fetch enrolments only when scope contains 'openid:hmrc_enrolments'") {

      Given("A Auth token with 'openid:hmrc_enrolments' scopes")
      thirdPartyDelegatedAuthorityStub.willReturnScopesForAuthBearerToken(authBearerToken,
        Set("openid:hmrc_enrolments"))

      And("The Auth token has a confidence level above 200 and a NINO")
      authStub.willReturnAuthorityWith(ConfidenceLevel.L200, Nino(nino))

      And("The authority has enrolments")
      authStub.willReturnEnrolmentsWith()

      When("We request the user information")
      val result = Http(s"$serviceUrl")
        .headers(Seq("Authorization" -> s"Bearer $authBearerToken", "Accept" -> "application/vnd.hmrc.1.0+json"))
        .asString

      Then("The user information is returned")
      result.code shouldBe 200
      val userWithEnrolmentsOnly = userInfo.copy(given_name = None, family_name = None, middle_name = None, address = None, birthdate = None, uk_gov_nino = None, government_gateway = None, email = None)
      Json.parse(result.body) shouldBe Json.toJson(userWithEnrolmentsOnly)
    }

    scenario("fetch government gateway details only when scope contains 'openid:government_gateway'") {

      Given("A Auth token with 'openid:government_gateway' scopes")
      thirdPartyDelegatedAuthorityStub.willReturnScopesForAuthBearerToken(authBearerToken,
        Set("openid:government_gateway"))

      And("The Auth token has a confidence level above 200 and a NINO")
      authStub.willReturnAuthorityWith(ConfidenceLevel.L200, Nino(nino))

      And("The authority has enrolments")
      authStub.willReturnEnrolmentsWith()

      And("UserDeails for the user")
      userDetailsStub.willReturnUserDetailsWith(email)

      When("We request the user information")
      val result = Http(s"$serviceUrl")
        .headers(Seq("Authorization" -> s"Bearer $authBearerToken", "Accept" -> "application/vnd.hmrc.1.0+json", "token" -> "ggToken"))
        .asString

      Then("The user information is returned")
      result.code shouldBe 200
      val userWithGovernmentDetailsOnly = userInfo.copy(given_name = None, family_name = None, middle_name = None, address = None, birthdate = None, uk_gov_nino = None, hmrc_enrolments = None, email = None)
      Json.parse(result.body) shouldBe Json.toJson(userWithGovernmentDetailsOnly)
    }

    scenario("return 401 - unauthorized when confidence level is less than 200") {

      Given("A Auth token with 'openid', 'profile', 'address', 'openid:gov-uk-identifiers' and 'openid:hmrc_enrolments' scopes")
      thirdPartyDelegatedAuthorityStub.willReturnScopesForAuthBearerToken(authBearerToken,
        Set("openid", "profile", "address", "openid:gov-uk-identifiers", "openid:hmrc_enrolments"))

      And("The Auth token has a confidence level above 200 and a NINO")
      authStub.willReturnAuthorityWith(ConfidenceLevel.L100, Nino(nino))

      When("We request the user information")
      val result = Http(s"$serviceUrl")
        .headers(Seq("Authorization" -> s"Bearer $authBearerToken", "Accept" -> "application/vnd.hmrc.1.0+json"))
        .asString

      Then("The user information is returned")
      result.code shouldBe 401

      Json.parse(result.body) shouldBe Json.parse(s"""{"code":"UNAUTHORIZED","message":"Bearer token is missing or not authorized"}""".stripMargin)
    }
  }
}
