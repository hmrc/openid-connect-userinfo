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

import java.nio.file.Paths

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.fge.jsonschema.core.report.LogLevel
import com.github.fge.jsonschema.main.JsonSchemaFactory
import com.github.tomakehurst.wiremock.client.RequestPatternBuilder
import org.joda.time.LocalDate
import org.scalatest.BeforeAndAfterAll
import play.api.libs.json.Json
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.auth.core.{AffinityGroup, _}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.openidconnect.userinfo.config.{FeatureSwitch, UserInfoFeatureSwitches}
import uk.gov.hmrc.openidconnect.userinfo.domain.{Address, DesUserInfo, GovernmentGatewayDetails, Mdtp, UserInfo}

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
  val desUserInfo = DesUserInfo(ItmpName(Some("John"), Some("A"), Some("Smith")), Some(LocalDate.parse("1980-01-01")),
    ItmpAddress(Some("1 Station Road"), Some("Town Centre"), Some("London"), Some("England"), Some("UK"), Some("NW1 6XE"), Some("GREAT BRITAIN"), Some("GB")))
  val enrolments = Set(Enrolment("IR-SA", List(EnrolmentIdentifier("UTR", "174371121")), "Activated"))
  val deviceId = "device-id-12345"
  val sessionId = "session-id-12345"
  val mdtp = Mdtp(deviceId, sessionId)
  val authMdtp = MdtpInformation(deviceId, sessionId)
  val gatewayInformation = GatewayInformation(Some("gateway-token-qwert"))
  val government_gateway: GovernmentGatewayDetails = GovernmentGatewayDetails(Some("1304372065861347"),Some(Seq("Admin")), Some("Bob")
    ,Some("Individual"), Some("AC-12345"), Some("ACC"), Some("AC Accounting"), Some("gateway-token-qwert"), Some(10))
  val email = "my-email@abc.uk"

  val userInfo = UserInfo(
    Some("John"),
    Some("Smith"),
    Some("A"),
    Some(Address("1 Station Road\nTown Centre\nLondon\nEngland\nUK\nNW1 6XE\nGREAT BRITAIN", Some("NW1 6XE"), Some("GREAT BRITAIN"), Some("GB"))),
    Some(email),
    Some(LocalDate.parse("1980-01-01")),
    Some("AB123456A"),
    Some(enrolments),
    Some(government_gateway),
    Some(mdtp))
  val desUserInfoWithoutFirstName = DesUserInfo(ItmpName(None, Some("A"), Some("Smith")), Some(LocalDate.parse("1980-01-01")),
    ItmpAddress(Some("1 Station Road"), Some("Town Centre"), Some("London"), Some("England"),  Some("UK"), Some("NW1 6XE"), Some("GREAT BRITAIN"), Some("GB")))
  val userInfoWithoutFirstName = UserInfo(
    None,
    Some("Smith"),
    Some("A"),
    Some(Address("1 Station Road\nTown Centre\nLondon\nEngland\nUK\nNW1 6XE\nGREAT BRITAIN", Some("NW1 6XE"), Some("GREAT BRITAIN"), Some("GB"))),
    Some(email),
    Some(LocalDate.parse("1980-01-01")),
    Some("AB123456A"),
    Some(enrolments),
    Some(government_gateway),
    Some(mdtp)
  )
  val desUserInfoWithoutFamilyName = DesUserInfo(ItmpName(Some("John"), Some("A"), None), Some(LocalDate.parse("1980-01-01")),
    ItmpAddress(Some("1 Station Road"), Some("Town Centre"), Some("London"), Some("England"),  Some("UK"), Some("NW1 6XE"), Some("GREAT BRITAIN"), Some("GB")))
  val userInfoWithoutFamilyName = UserInfo(
    Some("John"),
    None,
    Some("A"),
    Some(Address("1 Station Road\nTown Centre\nLondon\nEngland\nUK\nNW1 6XE\nGREAT BRITAIN", Some("NW1 6XE"), Some("GREAT BRITAIN"), Some("GB"))),
    Some(email),
    Some(LocalDate.parse("1980-01-01")),
    Some("AB123456A"),
    Some(enrolments),
    Some(government_gateway),
    Some(mdtp))
  val desUserInfoWithPartialAddress = DesUserInfo(ItmpName(Some("John"), Some("A"), Some("Smith")), Some(LocalDate.parse("1980-01-01")),
    ItmpAddress(Some("1 Station Road"), None, Some("Lancaster"), Some("England"), Some("UK"), Some("NW1 6XE"), Some("GREAT BRITAIN"), Some("GB")))
  val userInfoWithPartialAddress = UserInfo(
    Some("John"),
    Some("Smith"),
    Some("A"),
    Some(Address("1 Station Road\nLancaster\nEngland\nUK\nNW1 6XE\nGREAT BRITAIN", Some("NW1 6XE"), Some("GREAT BRITAIN"), Some("GB"))),
    Some(email),
    Some(LocalDate.parse("1980-01-01")),
    Some("AB123456A"),
    None,
    Some(government_gateway),
    Some(mdtp))

    feature("fetch user information") {

    scenario("fetch user profile") {

      Given("A Auth token with 'openid', 'profile', 'address', 'openid:gov-uk-identifiers', 'openid:hmrc-enrolments', 'openid:mdtp'," +
        "'email' and 'openid:government-gateway' scopes")
      thirdPartyDelegatedAuthorityStub.willReturnScopesForAuthBearerToken(authBearerToken,
        Set("openid", "profile", "address", "openid:gov-uk-identifiers", "openid:hmrc-enrolments",
          "openid:government-gateway", "email", "agentInformation", "openid:mdtp"))
      authStub.willAuthoriseWith(200)

      And("The Auth token has a NINO")
      authStub.willReturnAuthorityWith(Nino(nino))

      And("The authority has enrolments")
      authStub.willReturnEnrolmentsWith()

      And("The auth will authorise DES contains user information for the NINO")
      authStub.willFindUser(Some(desUserInfo), Some(AgentInformation(government_gateway.agent_id, government_gateway.agent_code, government_gateway.agent_friendly_name)), Some(Credentials("1304372065861347", "")),
        Some(uk.gov.hmrc.auth.core.retrieve.Name(Some("Bob"), None)), Some(Email(email)), Some(AffinityGroup.Individual),
        Some(Admin), Some(authMdtp), Some(gatewayInformation), Some(10))

      When("We request the user information")
      val result = Http(s"$serviceUrl")
        .headers(Seq("Authorization" -> s"Bearer $authBearerToken", "Accept" -> "application/vnd.hmrc.1.0+json", "token" -> "ggToken"))
        .asString

      val validator = JsonSchemaFactory.byDefault().getValidator
      val root = System.getProperty("user.dir")
      val public10 = Paths.get(root, "public", "api", "conf", "1.0").toString
      val mapper = new ObjectMapper

      val schema = mapper.readTree(Paths.get(public10, "schemas", "userinfo.json").toFile)
      println(result.body)
      val json = Json.parse(result.body)

      val report = validator.validate(schema, mapper.readTree(json.toString()))

      System.out.println(authStub.stub.mock.find(RequestPatternBuilder.allRequests()))
      Then("The user information is returned")
      result.code shouldBe 200

      import scala.collection.JavaConversions._
      assert(report.isSuccess, report.filter(_.getLogLevel == LogLevel.ERROR).map(m => m))

      json shouldBe Json.toJson(userInfo)
    }

    scenario("fetch user profile without family name") {

      Given("A Auth token with 'openid', 'profile', 'address', 'openid:gov-uk-identifiers' and 'openid:hmrc-enrolments' scopes")
      thirdPartyDelegatedAuthorityStub.willReturnScopesForAuthBearerToken(authBearerToken,
        Set("openid", "profile", "address", "openid:gov-uk-identifiers", "openid:hmrc-enrolments"))
      authStub.willAuthoriseWith(200)

      And("The Auth token has a NINO")
      authStub.willReturnAuthorityWith(Nino(nino))

      And("The authority has enrolments")
      authStub.willReturnEnrolmentsWith()

      And("The auth will authorise and DES contains user information for the NINO")
      authStub.willFindUser(Some(desUserInfoWithoutFamilyName))


      When("We request the user information")
      val result = Http(s"$serviceUrl")
        .headers(Seq("Authorization" -> s"Bearer $authBearerToken", "Accept" -> "application/vnd.hmrc.1.0+json"))
        .asString

      Then("The user information is returned")
      result.code shouldBe 200
      Json.parse(result.body) shouldBe Json.toJson(userInfoWithoutFamilyName.copy(government_gateway = None, email = None,
        mdtp = None))
    }

    scenario("fetch user data without enrolments when there are no enrolments") {

      Given("A Auth token with 'openid', 'profile', 'address', 'openid:gov-uk-identifiers', 'email', 'openid:government-gateway' and 'openid:hmrc-enrolments' scopes")
      thirdPartyDelegatedAuthorityStub.willReturnScopesForAuthBearerToken(authBearerToken,
        Set("openid", "profile", "address", "openid:gov-uk-identifiers", "openid:hmrc-enrolments", "openid:government-gateway", "email", "openid:mdtp"))
      authStub.willAuthoriseWith(200)

      And("The Auth token has a NINO")
      authStub.willReturnAuthorityWith(Nino(nino))

      And("The auth will authorise and DES contains user information for the NINO")
      authStub.willFindUser(Some(desUserInfo), Some(AgentInformation(government_gateway.agent_id, government_gateway.agent_code, government_gateway.agent_friendly_name)), Some(Credentials("", "")),
        Some(uk.gov.hmrc.auth.core.retrieve.Name(Some("Bob"), None)), Some(Email(email)), Some(AffinityGroup.Individual), Some(Admin), Some(authMdtp), Some(gatewayInformation), Some(10))

      When("We request the user information")
      val result = Http(s"$serviceUrl")
        .headers(Seq("Authorization" -> s"Bearer $authBearerToken", "Accept" -> "application/vnd.hmrc.1.0+json", "token" -> "ggToken"))
        .asString

      Then("The user information is returned")
      result.code shouldBe 200
      Json.parse(result.body) shouldBe Json.toJson(userInfo.copy(hmrc_enrolments = None))
    }

    scenario("fetch user data without address and user details when there are no address and user details") {

      Given("A Auth token with 'openid', 'profile', 'address', 'openid:gov-uk-identifiers' and 'openid:hmrc-enrolments' scopes")
      thirdPartyDelegatedAuthorityStub.willReturnScopesForAuthBearerToken(authBearerToken,
        Set("openid", "profile", "address", "openid:gov-uk-identifiers", "openid:hmrc-enrolments"))
      authStub.willAuthoriseWith(200)

      And("The Auth token has a NINO")
      authStub.willReturnAuthorityWith(Nino(nino))

      And("The authority has enrolments")
      authStub.willReturnEnrolmentsWith()

      And("The auth will authorise and DES contains user information for the NINO")
      authStub.willNotFindUser()

      When("We request the user information")
      val result = Http(s"$serviceUrl")
        .headers(Seq("Authorization" -> s"Bearer $authBearerToken", "Accept" -> "application/vnd.hmrc.1.0+json"))
        .asString

      Then("The user information is returned")
      result.code shouldBe 200
      val userWithNinoAndEnrolmentsOnly = userInfo.copy(given_name = None, family_name = None, middle_name = None,
        address = None, birthdate = None, government_gateway = None, email = None, mdtp = None)
      Json.parse(result.body) shouldBe Json.toJson(userWithNinoAndEnrolmentsOnly)
    }

    scenario("fetch enrolments only when scope contains 'openid:hmrc-enrolments'") {

      Given("A Auth token with 'openid:hmrc-enrolments' scopes")
      thirdPartyDelegatedAuthorityStub.willReturnScopesForAuthBearerToken(authBearerToken,
        Set("openid:hmrc-enrolments"))
      authStub.willAuthoriseWith(200)

      And("The Auth token has a NINO")
      authStub.willReturnAuthorityWith(Nino(nino))

      And("The authority has enrolments")
      authStub.willReturnEnrolmentsWith()

      When("We request the user information")
      val result = Http(s"$serviceUrl")
        .headers(Seq("Authorization" -> s"Bearer $authBearerToken", "Accept" -> "application/vnd.hmrc.1.0+json"))
        .asString

      Then("The user information is returned")
      result.code shouldBe 200
      val userWithEnrolmentsOnly = userInfo.copy(given_name = None, family_name = None, middle_name = None,
        address = None, birthdate = None, uk_gov_nino = None, government_gateway = None, email = None, mdtp = None)
      Json.parse(result.body) shouldBe Json.toJson(userWithEnrolmentsOnly)
    }

    scenario("fetch government gateway details only when scope contains 'openid:government-gateway'") {

      Given("A Auth token with 'openid:government-gateway' scopes")
      thirdPartyDelegatedAuthorityStub.willReturnScopesForAuthBearerToken(authBearerToken,
        Set("openid:government-gateway"))
      authStub.willAuthoriseWith(200)

      And("The Auth token has a NINO")
      authStub.willReturnAuthorityWith(Nino(nino))

      And("The authority has enrolments")
      authStub.willReturnEnrolmentsWith()

      And("The auth will authorise DES contains user information for the NINO")
      authStub.willFindUser(Some(desUserInfo), Some(AgentInformation(government_gateway.agent_id, government_gateway.agent_code,
        government_gateway.agent_friendly_name)), Some(Credentials("1304372065861347", "")),
        Some(uk.gov.hmrc.auth.core.retrieve.Name(Some("Bob"), None)), Some(Email(email)), Some(AffinityGroup.Individual),
        Some(Admin), None, gatewayInformation = Some(gatewayInformation), Some(10))

      When("We request the user information")
      val result = Http(s"$serviceUrl")
        .headers(Seq("Authorization" -> s"Bearer $authBearerToken", "Accept" -> "application/vnd.hmrc.1.0+json", "token" -> "ggToken"))
        .asString

      Then("The user information is returned")
      result.code shouldBe 200
      val userWithGovernmentDetailsOnly = userInfo.copy(given_name = None, family_name = None, middle_name = None,
        address = None, birthdate = None, uk_gov_nino = None, hmrc_enrolments = None, email = None, mdtp = None)
      Json.parse(result.body) shouldBe Json.toJson(userWithGovernmentDetailsOnly)
    }

  }

  feature("fetching user information propagates Unauthorized errors from upstream services") {

    scenario("return 401 when Auth returns Unauthorized") {
      Given("A Auth token with openid:government-gateway, openid:hmrc-enrolments, address scopes")
      thirdPartyDelegatedAuthorityStub.willReturnScopesForAuthBearerToken(authBearerToken,
        Set("openid:government-gateway", "openid:hmrc-enrolments", "address"))

      And("Auth returns unauthorized")
      authStub.willAuthoriseWith(401)

      When("We request the user information")
      val result = Http(s"$serviceUrl")
        .headers(Seq("Authorization" -> s"Bearer $authBearerToken", "Accept" -> "application/vnd.hmrc.1.0+json"))
        .asString

      Then("Unauthorized status is returned")
      result.code shouldBe 401
    }
  }

  feature("fetching user information handles upstream errors") {

    scenario("return 502 when Auth returns error") {
      val errorMsg = "auth error msg"
      val expectedErrorMessage = s"""{"code":"BAD_GATEWAY","message":"POST of 'http://localhost:22221/auth/authorise' returned 503. Response body: '$errorMsg'"}"""
      Given("A Auth token with openid:government-gateway, openid:hmrc-enrolments, address scopes")
      thirdPartyDelegatedAuthorityStub.willReturnScopesForAuthBearerToken(authBearerToken,
        Set("openid:government-gateway", "openid:hmrc-enrolments", "address"))

      And("Auth returns unauthorized")
      authStub.willAuthoriseWith(503, errorMsg)

      When("We request the user information")
      val result = Http(s"$serviceUrl")
        .headers(Seq("Authorization" -> s"Bearer $authBearerToken", "Accept" -> "application/vnd.hmrc.1.0+json"))
        .asString

      Then("Bad gateway status is returned")
      result.code shouldBe 502
      Json.parse(result.body) shouldBe Json.parse(expectedErrorMessage)
    }

    scenario("return 502 when Auth returns not found") {
      val errorMsg = "auth error msg"
      val expectedErrorMessage = s"""{"code":"BAD_GATEWAY","message":"POST of 'http://localhost:22221/auth/authorise' returned 404 (Not Found). Response body: '$errorMsg'"}"""
      Given("A Auth token with openid:government-gateway, openid:hmrc-enrolments, address scopes")
      thirdPartyDelegatedAuthorityStub.willReturnScopesForAuthBearerToken(authBearerToken,
        Set("openid:government-gateway", "openid:hmrc-enrolments", "address"))

      And("Auth returns not found")
      authStub.willAuthoriseWith(404, errorMsg)

      When("We request the user information")
      val result = Http(s"$serviceUrl")
        .headers(Seq("Authorization" -> s"Bearer $authBearerToken", "Accept" -> "application/vnd.hmrc.1.0+json"))
        .asString

      Then("Bad gateway status is returned")
      result.code shouldBe 502
      Json.parse(result.body) shouldBe Json.parse(expectedErrorMessage)
    }
  }
}
