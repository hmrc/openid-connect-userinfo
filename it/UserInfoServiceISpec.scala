/*
 * Copyright 2023 HM Revenue & Customs
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

import java.nio.file.Paths
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.fge.jsonschema.core.report.LogLevel
import com.github.fge.jsonschema.main.JsonSchemaFactory
import config.{FeatureSwitch, UserInfoFeatureSwitches}
import domain._
import java.time.LocalDate
import play.api.libs.json.Json
import scalaj.http.{Http, HttpOptions}
import stubs.{AuthStub, ThirdPartyDelegatedAuthorityStub}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.domain.Nino

class UserInfoServiceISpec extends BaseFeatureISpec with AuthStub with ThirdPartyDelegatedAuthorityStub {

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    wireMockServer.resetMappings()
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    FeatureSwitch.enable(UserInfoFeatureSwitches.countryCode)
    FeatureSwitch.enable(UserInfoFeatureSwitches.addressLine5)
  }
  override def afterAll(): Unit = {
    super.afterAll()
    FeatureSwitch.disable(UserInfoFeatureSwitches.countryCode)
    FeatureSwitch.disable(UserInfoFeatureSwitches.addressLine5)
  }

  val serviceUrl: String = resource("")

  val authorizationTokens = "AUTHORIZATION_TOKENS"
  val accessToken = "ACCESS_TOKEN"
  val nino = "AB123456A"
  val ukCountryCode = 1
  val desUserInfo = DesUserInfo(
    ItmpName(Some("John"), Some("A"), Some("Smith")),
    Some(LocalDate.parse("1980-01-01")),
    ItmpAddress(Some("1 Station Road"),
                Some("Town Centre"),
                Some("London"),
                Some("England"),
                Some("UK"),
                Some("NW1 6XE"),
                Some("GREAT BRITAIN"),
                Some("GB")
               )
  )
  val enrolments: Set[Enrolment] = Set(Enrolment("IR-SA", List(EnrolmentIdentifier("UTR", "174371121")), "Activated"))
  val deviceId = "device-id-12345"
  val sessionId = "session-id-12345"
  val mdtp = Mdtp(deviceId, sessionId)
  val authMdtp = MdtpInformation(deviceId, sessionId)
  val gatewayInformation = GatewayInformation(Some("gateway-token-qwert"))
  val government_gateway_v1: GovernmentGatewayDetails = GovernmentGatewayDetails(
    Some("1304372065861347"),
    Some(Seq("User")),
    Some("Bob"),
    Some("Individual"),
    Some("AC-12345"),
    Some("ACC"),
    Some("AC Accounting"),
    Some("gateway-token-qwert"),
    None,
    None,
    None
  )
  val government_gateway_v2: GovernmentGatewayDetails = GovernmentGatewayDetails(
    Some("1304372065861347"),
    Some(Seq("User")),
    Some("Bob"),
    Some("Individual"),
    Some("AC-12345"),
    Some("ACC"),
    Some("AC Accounting"),
    Some("gateway-token-qwert"),
    None,
    Some("some_url"),
    Some("some_other_url")
  )
  val email = "my-email@abc.uk"

  val userInfo_v1 = UserInfo(
    Some("John"),
    Some("Smith"),
    Some("A"),
    Some(Address("1 Station Road\nTown Centre\nLondon\nEngland\nUK\nNW1 6XE\nGREAT BRITAIN", Some("NW1 6XE"), Some("GREAT BRITAIN"), Some("GB"))),
    Some(email),
    Some(LocalDate.parse("1980-01-01")),
    Some("AB123456A"),
    Some(enrolments),
    Some(government_gateway_v1),
    Some(mdtp)
  )

  val userInfo_v2 = UserInfo(
    Some("John"),
    Some("Smith"),
    Some("A"),
    Some(Address("1 Station Road\nTown Centre\nLondon\nEngland\nUK\nNW1 6XE\nGREAT BRITAIN", Some("NW1 6XE"), Some("GREAT BRITAIN"), Some("GB"))),
    Some(email),
    Some(LocalDate.parse("1980-01-01")),
    Some("AB123456A"),
    Some(enrolments),
    Some(government_gateway_v2),
    Some(mdtp)
  )

  val desUserInfoWithoutFirstName = DesUserInfo(
    ItmpName(None, Some("A"), Some("Smith")),
    Some(LocalDate.parse("1980-01-01")),
    ItmpAddress(Some("1 Station Road"),
                Some("Town Centre"),
                Some("London"),
                Some("England"),
                Some("UK"),
                Some("NW1 6XE"),
                Some("GREAT BRITAIN"),
                Some("GB")
               )
  )
  val userInfoWithoutFirstName = UserInfo(
    None,
    Some("Smith"),
    Some("A"),
    Some(Address("1 Station Road\nTown Centre\nLondon\nEngland\nUK\nNW1 6XE\nGREAT BRITAIN", Some("NW1 6XE"), Some("GREAT BRITAIN"), Some("GB"))),
    Some(email),
    Some(LocalDate.parse("1980-01-01")),
    Some("AB123456A"),
    Some(enrolments),
    Some(government_gateway_v1),
    Some(mdtp)
  )
  val desUserInfoWithoutFamilyName = DesUserInfo(
    ItmpName(Some("John"), Some("A"), None),
    Some(LocalDate.parse("1980-01-01")),
    ItmpAddress(Some("1 Station Road"),
                Some("Town Centre"),
                Some("London"),
                Some("England"),
                Some("UK"),
                Some("NW1 6XE"),
                Some("GREAT BRITAIN"),
                Some("GB")
               )
  )
  val userInfoWithoutFamilyName = UserInfo(
    Some("John"),
    None,
    Some("A"),
    Some(Address("1 Station Road\nTown Centre\nLondon\nEngland\nUK\nNW1 6XE\nGREAT BRITAIN", Some("NW1 6XE"), Some("GREAT BRITAIN"), Some("GB"))),
    Some(email),
    Some(LocalDate.parse("1980-01-01")),
    Some("AB123456A"),
    Some(enrolments),
    Some(government_gateway_v1),
    Some(mdtp)
  )
  val desUserInfoWithPartialAddress = DesUserInfo(
    ItmpName(Some("John"), Some("A"), Some("Smith")),
    Some(LocalDate.parse("1980-01-01")),
    ItmpAddress(Some("1 Station Road"), None, Some("Lancaster"), Some("England"), Some("UK"), Some("NW1 6XE"), Some("GREAT BRITAIN"), Some("GB"))
  )
  val userInfoWithPartialAddress = UserInfo(
    Some("John"),
    Some("Smith"),
    Some("A"),
    Some(Address("1 Station Road\nLancaster\nEngland\nUK\nNW1 6XE\nGREAT BRITAIN", Some("NW1 6XE"), Some("GREAT BRITAIN"), Some("GB"))),
    Some(email),
    Some(LocalDate.parse("1980-01-01")),
    Some("AB123456A"),
    None,
    Some(government_gateway_v1),
    Some(mdtp)
  )

  Feature("fetch user information") {

    Scenario("fetch user profile v1") {

      Given(
        "A Auth token with 'openid', 'profile', 'address', 'openid:gov-uk-identifiers', 'openid:hmrc-enrolments', 'openid:mdtp'," +
          "'email' and 'openid:government-gateway' scopes"
      )
      willReturnScopesForAuthTokens(
        accessToken,
        Set(
          "openid",
          "profile",
          "address",
          "openid:gov-uk-identifiers",
          "openid:hmrc-enrolments",
          "openid:government-gateway",
          "email",
          "agentInformation",
          "openid:mdtp"
        )
      )
      willAuthoriseWith(200)

      And("The Auth token has a NINO")
      willReturnAuthorityWith(Nino(nino))

      And("The authority has enrolments")
      willReturnEnrolmentsWith()

      And("The auth will authorise DES contains user information for the NINO")
      willFindUser(
        Some(desUserInfo),
        Some(AgentInformation(government_gateway_v1.agent_id, government_gateway_v1.agent_code, government_gateway_v1.agent_friendly_name)),
        Some(Credentials("1304372065861347", "")),
        Some(uk.gov.hmrc.auth.core.retrieve.Name(Some("Bob"), None)),
        Some(Email(email)),
        Some(AffinityGroup.Individual),
        Some(User),
        Some(authMdtp),
        Some(gatewayInformation),
        Some(10)
      )

      When("We request the user information")
      val result = Http(s"$serviceUrl")
        .headers(
          Seq(
            "Authorization"                -> s"Bearer $authorizationTokens",
            "Accept"                       -> "application/vnd.hmrc.1.0+json",
            "token"                        -> "ggToken",
            "X-Client-Authorization-Token" -> "ACCESS_TOKEN"
          )
        )
        .asString

      val validator = JsonSchemaFactory.byDefault().getValidator
      val mapper = new ObjectMapper

      val schema = mapper.readTree(Paths.get(getClass.getResource("1.0/schemas/userinfo.json").toURI).toFile)
      val json = Json.parse(result.body)

      val report = validator.validate(schema, mapper.readTree(json.toString()))

      Then("The user information is returned")
      result.code shouldBe 200

      import scala.jdk.CollectionConverters._
      assert(report.isSuccess, report.asScala.filter(_.getLogLevel == LogLevel.ERROR).map(m => m))

      json shouldBe Json.toJson(userInfo_v1)
    }

    Scenario("fetch user profile but bad accept header") {

      Given(
        "A Auth token with 'openid', 'profile', 'address', 'openid:gov-uk-identifiers', 'openid:hmrc-enrolments', 'openid:mdtp'," +
          "'email' and 'openid:government-gateway' scopes"
      )
      willReturnScopesForAuthTokens(
        accessToken,
        Set(
          "openid",
          "profile",
          "address",
          "openid:gov-uk-identifiers",
          "openid:hmrc-enrolments",
          "openid:government-gateway",
          "email",
          "agentInformation",
          "openid:mdtp"
        )
      )
      willAuthoriseWith(200)

      And("The Auth token has a NINO")
      willReturnAuthorityWith(Nino(nino))

      And("The authority has enrolments")
      willReturnEnrolmentsWith()

      And("The auth will authorise DES contains user information for the NINO")
      willFindUser(
        Some(desUserInfo),
        Some(AgentInformation(government_gateway_v1.agent_id, government_gateway_v1.agent_code, government_gateway_v1.agent_friendly_name)),
        Some(Credentials("1304372065861347", "")),
        Some(uk.gov.hmrc.auth.core.retrieve.Name(Some("Bob"), None)),
        Some(Email(email)),
        Some(AffinityGroup.Individual),
        Some(User),
        Some(authMdtp),
        Some(gatewayInformation),
        Some(10)
      )

      When("We request the user information")
      val result = Http(s"$serviceUrl")
        .headers(
          Seq(
            "Authorization"                -> s"Bearer $authorizationTokens",
            "Accept"                       -> "application/vnd.hmrc.2.0+json",
            "token"                        -> "ggToken",
            "X-Client-Authorization-Token" -> "ACCESS_TOKEN"
          )
        )
        .asString

      Then("return Not Acceptable http response")
      result.code shouldBe 406
    }

    Scenario("fetch user profile v1 when Accept Header is missing") {

      Given(
        "A Auth token with 'openid', 'profile', 'address', 'openid:gov-uk-identifiers', 'openid:hmrc-enrolments', 'openid:mdtp'," +
          "'email' and 'openid:government-gateway' scopes"
      )
      willReturnScopesForAuthTokens(
        accessToken,
        Set(
          "openid",
          "profile",
          "address",
          "openid:gov-uk-identifiers",
          "openid:hmrc-enrolments",
          "openid:government-gateway",
          "email",
          "agentInformation",
          "openid:mdtp"
        )
      )
      willAuthoriseWith(200)

      And("The Auth token has a NINO")
      willReturnAuthorityWith(Nino(nino))

      And("The authority has enrolments")
      willReturnEnrolmentsWith()

      And("The auth will authorise DES contains user information for the NINO")
      willFindUser(
        Some(desUserInfo),
        Some(AgentInformation(government_gateway_v1.agent_id, government_gateway_v1.agent_code, government_gateway_v1.agent_friendly_name)),
        Some(Credentials("1304372065861347", "")),
        Some(uk.gov.hmrc.auth.core.retrieve.Name(Some("Bob"), None)),
        Some(Email(email)),
        Some(AffinityGroup.Individual),
        Some(User),
        Some(authMdtp),
        Some(gatewayInformation),
        Some(10)
      )

      When("We request the user information")
      val result = Http(s"$serviceUrl")
        .headers(
          Seq(
            "Authorization"                -> s"Bearer $authorizationTokens",
            "token"                        -> "ggToken",
            "X-Client-Authorization-Token" -> "ACCESS_TOKEN",
            "Accept" -> "" // "" is needed to pretend it is missing as test http libraries (such as scalaj.http and play.api.http) inject default Accept header if this is absent
          )
        )
        .asString

      val validator = JsonSchemaFactory.byDefault().getValidator
      val mapper = new ObjectMapper

      val schema = mapper.readTree(Paths.get(getClass.getResource("1.0/schemas/userinfo.json").toURI).toFile)
      val json = Json.parse(result.body)

      val report = validator.validate(schema, mapper.readTree(json.toString()))

      Then("The user information is returned")
      result.code shouldBe 200

      import scala.jdk.CollectionConverters._
      assert(report.isSuccess, report.asScala.filter(_.getLogLevel == LogLevel.ERROR).map(m => m))

      json shouldBe Json.toJson(userInfo_v1)
    }

    Scenario("fetch user profile without family name") {

      Given("A Auth token with 'openid', 'profile', 'address', 'openid:gov-uk-identifiers' and 'openid:hmrc-enrolments' scopes")
      willReturnScopesForAuthTokens(accessToken, Set("openid", "profile", "address", "openid:gov-uk-identifiers", "openid:hmrc-enrolments"))
      willAuthoriseWith(200)

      And("The Auth token has a NINO")
      willReturnAuthorityWith(Nino(nino))

      And("The authority has enrolments")
      willReturnEnrolmentsWith()

      And("The auth will authorise and DES contains user information for the NINO")
      willFindUser(Some(desUserInfoWithoutFamilyName))

      When("We request the user information")
      val result = Http(s"$serviceUrl")
        .headers(
          Seq("Authorization"                -> s"Bearer $authorizationTokens",
              "Accept"                       -> "application/vnd.hmrc.1.0+json",
              "X-Client-Authorization-Token" -> "ACCESS_TOKEN"
             )
        )
        .asString

      Then("The user information is returned")
      result.code             shouldBe 200
      Json.parse(result.body) shouldBe Json.toJson(userInfoWithoutFamilyName.copy(government_gateway = None, email = None, mdtp = None))
    }

    Scenario("fetch user data without enrolments when there are no enrolments") {

      Given(
        "A Auth token with 'openid', 'profile', 'address', 'openid:gov-uk-identifiers', 'email', 'openid:government-gateway' and 'openid:hmrc-enrolments' scopes"
      )
      willReturnScopesForAuthTokens(
        accessToken,
        Set("openid",
            "profile",
            "address",
            "openid:gov-uk-identifiers",
            "openid:hmrc-enrolments",
            "openid:government-gateway",
            "email",
            "openid:mdtp"
           )
      )
      willAuthoriseWith(200)

      And("The Auth token has a NINO")
      willReturnAuthorityWith(Nino(nino))

      And("The auth will authorise and DES contains user information for the NINO")
      willFindUser(
        Some(desUserInfo),
        Some(AgentInformation(government_gateway_v1.agent_id, government_gateway_v1.agent_code, government_gateway_v1.agent_friendly_name)),
        Some(Credentials("", "")),
        Some(uk.gov.hmrc.auth.core.retrieve.Name(Some("Bob"), None)),
        Some(Email(email)),
        Some(AffinityGroup.Individual),
        Some(User),
        Some(authMdtp),
        Some(gatewayInformation),
        Some(10)
      )

      When("We request the user information with x-client-authorization-token header name in lowercase")
      val result = Http(s"$serviceUrl")
        .headers(
          Seq(
            "Authorization"                -> s"Bearer $authorizationTokens",
            "Accept"                       -> "application/vnd.hmrc.1.0+json",
            "token"                        -> "ggToken",
            "x-client-authorization-token" -> "ACCESS_TOKEN"
          )
        )
        .asString

      Then("The user information is returned")
      result.code             shouldBe 200
      Json.parse(result.body) shouldBe Json.toJson(userInfo_v1.copy(hmrc_enrolments = None))
    }

    Scenario("fetch user data without address and user details when there are no address and user details") {

      Given("A Auth token with 'openid', 'profile', 'address', 'openid:gov-uk-identifiers' and 'openid:hmrc-enrolments' scopes")
      willReturnScopesForAuthTokens(accessToken, Set("openid", "profile", "address", "openid:gov-uk-identifiers", "openid:hmrc-enrolments"))
      willAuthoriseWith(200)

      And("The Auth token has a NINO")
      willReturnAuthorityWith(Nino(nino))

      And("The authority has enrolments")
      willReturnEnrolmentsWith()

      And("The auth will authorise and DES contains user information for the NINO")
      willNotFindUser()

      When("We request the user information")
      val result = Http(s"$serviceUrl")
        .headers(
          Seq("Authorization"                -> s"Bearer $authorizationTokens",
              "Accept"                       -> "application/vnd.hmrc.1.0+json",
              "X-Client-Authorization-Token" -> "ACCESS_TOKEN"
             )
        )
        .asString

      Then("The user information is returned")
      result.code shouldBe 200
      val userWithNinoAndEnrolmentsOnly = userInfo_v1.copy(given_name         = None,
                                                           family_name        = None,
                                                           middle_name        = None,
                                                           address            = None,
                                                           birthdate          = None,
                                                           government_gateway = None,
                                                           email              = None,
                                                           mdtp               = None
                                                          )
      Json.parse(result.body) shouldBe Json.toJson(userWithNinoAndEnrolmentsOnly)
    }

    Scenario("fetch enrolments only when scope contains 'openid:hmrc-enrolments'") {

      Given("A Auth token with 'openid:hmrc-enrolments' scopes")
      willReturnScopesForAuthTokens(accessToken, Set("openid:hmrc-enrolments"))
      willAuthoriseWith(200)

      And("The Auth token has a NINO")
      willReturnAuthorityWith(Nino(nino))

      And("The authority has enrolments")
      willReturnEnrolmentsWith()

      When("We request the user information")
      val result = Http(s"$serviceUrl")
        .headers(
          Seq("Authorization"                -> s"Bearer $authorizationTokens",
              "Accept"                       -> "application/vnd.hmrc.1.0+json",
              "X-Client-Authorization-Token" -> "ACCESS_TOKEN"
             )
        )
        .asString

      Then("The user information is returned")
      result.code shouldBe 200
      val userWithEnrolmentsOnly = userInfo_v1.copy(given_name         = None,
                                                    family_name        = None,
                                                    middle_name        = None,
                                                    address            = None,
                                                    birthdate          = None,
                                                    uk_gov_nino        = None,
                                                    government_gateway = None,
                                                    email              = None,
                                                    mdtp               = None
                                                   )
      Json.parse(result.body) shouldBe Json.toJson(userWithEnrolmentsOnly)
    }

    Scenario("fetch government gateway details only when scope contains 'openid:government-gateway'") {

      Given("A Auth token with 'openid:government-gateway' scopes")
      willReturnScopesForAuthTokens(accessToken, Set("openid:government-gateway"))
      willAuthoriseWith(200)

      And("The Auth token has a NINO")
      willReturnAuthorityWith(Nino(nino))

      And("The authority has enrolments")
      willReturnEnrolmentsWith()

      And("The auth will authorise DES contains user information for the NINO")
      willFindUser(
        Some(desUserInfo),
        Some(AgentInformation(government_gateway_v1.agent_id, government_gateway_v1.agent_code, government_gateway_v1.agent_friendly_name)),
        Some(Credentials("1304372065861347", "")),
        Some(uk.gov.hmrc.auth.core.retrieve.Name(Some("Bob"), None)),
        Some(Email(email)),
        Some(AffinityGroup.Individual),
        Some(User),
        None,
        gatewayInformation = Some(gatewayInformation),
        Some(10)
      )

      When("We request the user information")
      val result = Http(s"$serviceUrl")
        .headers(
          Seq(
            "Authorization"                -> s"Bearer $authorizationTokens",
            "Accept"                       -> "application/vnd.hmrc.1.0+json",
            "token"                        -> "ggToken",
            "X-Client-Authorization-Token" -> "ACCESS_TOKEN"
          )
        )
        .asString

      Then("The user information is returned")
      result.code shouldBe 200
      val userWithGovernmentDetailsOnly = userInfo_v1.copy(given_name      = None,
                                                           family_name     = None,
                                                           middle_name     = None,
                                                           address         = None,
                                                           birthdate       = None,
                                                           uk_gov_nino     = None,
                                                           hmrc_enrolments = None,
                                                           email           = None,
                                                           mdtp            = None
                                                          )
      Json.parse(result.body) shouldBe Json.toJson(userWithGovernmentDetailsOnly)
    }
  }

  Feature("fetching user information propagates Unauthorized errors from upstream services") {

    Scenario("return 401 when Auth returns Unauthorized") {
      Given("A Auth token with openid:government-gateway, openid:hmrc-enrolments, address scopes")
      willReturnScopesForAuthTokens(accessToken, Set("openid:government-gateway", "openid:hmrc-enrolments", "address"))

      And("Auth returns unauthorized")
      willAuthoriseWith(401)

      When("We request the user information")
      val result = Http(s"$serviceUrl")
        .headers(
          Seq("Authorization"                -> s"Bearer $authorizationTokens",
              "Accept"                       -> "application/vnd.hmrc.1.0+json",
              "X-Client-Authorization-Token" -> "ACCESS_TOKEN"
             )
        )
        .asString

      Then("Unauthorized status is returned")
      result.code shouldBe 401
    }
  }

  Feature("fetching user information handles upstream errors") {

    Scenario("return 502 when Auth returns error") {
      val errorMsg = "auth error msg"
      val expectedErrorMessage =
        s"""{"code":"BAD_GATEWAY","message":"POST of 'http://localhost:$stubPort/auth/authorise' returned 503. Response body: '$errorMsg'"}"""
      Given("A Auth token with openid:government-gateway, openid:hmrc-enrolments, address scopes")
      willReturnScopesForAuthTokens(accessToken, Set("openid:government-gateway", "openid:hmrc-enrolments", "address"))

      And("Auth returns unauthorized")
      willAuthoriseWith(503, errorMsg)

      When("We request the user information")
      val result = Http(s"$serviceUrl")
        .headers(
          Seq("Authorization"                -> s"Bearer $authorizationTokens",
              "Accept"                       -> "application/vnd.hmrc.1.0+json",
              "X-Client-Authorization-Token" -> "ACCESS_TOKEN"
             )
        )
        .asString

      Then("Bad gateway status is returned")
      result.code             shouldBe 502
      Json.parse(result.body) shouldBe Json.parse(expectedErrorMessage)
    }

    Scenario("return 502 when Auth returns not found") {
      val errorMsg = "auth error msg"
      val expectedErrorMessage =
        s"""{"code":"BAD_GATEWAY","message":"POST of 'http://localhost:$stubPort/auth/authorise' returned 404 (Not Found). Response body: '$errorMsg'"}"""
      Given("A Auth token with openid:government-gateway, openid:hmrc-enrolments, address scopes")
      willReturnScopesForAuthTokens(accessToken, Set("openid:government-gateway", "openid:hmrc-enrolments", "address"))

      And("Auth returns not found")
      willAuthoriseWith(404, errorMsg)

      When("We request the user information")
      val result = Http(s"$serviceUrl")
        .headers(
          Seq("Authorization"                -> s"Bearer $authorizationTokens",
              "Accept"                       -> "application/vnd.hmrc.1.0+json",
              "X-Client-Authorization-Token" -> "ACCESS_TOKEN"
             )
        )
        .asString

      Then("Bad gateway status is returned")
      result.code             shouldBe 502
      Json.parse(result.body) shouldBe Json.parse(expectedErrorMessage)
    }

    Scenario("fetching user info returns 5xx then we return 502") {

      Given(
        "A Auth token with 'openid', 'profile', 'address', 'openid:gov-uk-identifiers', 'openid:hmrc-enrolments', 'openid:mdtp'," +
          "'email' and 'openid:government-gateway' scopes"
      )
      willReturnScopesForAuthTokens(
        accessToken,
        Set(
          "openid",
          "profile",
          "address",
          "openid:gov-uk-identifiers",
          "openid:hmrc-enrolments",
          "openid:government-gateway",
          "email",
          "agentInformation",
          "openid:mdtp"
        )
      )
      willAuthoriseWith(200)

      And("The Auth token has a NINO")
      willReturnAuthorityWith(Nino(nino))

      And("The authority has enrolments")
      willReturnEnrolmentsWith()

      And("The auth will user details call will fail with 500")
      willFindUserFailed(500)

      When("We request the user information")
      val result = Http(s"$serviceUrl")
        .options(HttpOptions.readTimeout(1000000), HttpOptions.connTimeout(1000000))
        .headers(
          Seq(
            "Authorization"                -> s"Bearer $authorizationTokens",
            "Accept"                       -> "application/vnd.hmrc.1.0+json",
            "token"                        -> "ggToken",
            "X-Client-Authorization-Token" -> "ACCESS_TOKEN"
          )
        )
        .asString

      Then("The user information is returned")
      result.code shouldBe 502

    }

    Scenario("fetching user info returns 4xx then we return 502") {

      Given(
        "A Auth token with 'openid', 'profile', 'address', 'openid:gov-uk-identifiers', 'openid:hmrc-enrolments', 'openid:mdtp'," +
          "'email' and 'openid:government-gateway' scopes"
      )
      willReturnScopesForAuthTokens(
        accessToken,
        Set(
          "openid",
          "profile",
          "address",
          "openid:gov-uk-identifiers",
          "openid:hmrc-enrolments",
          "openid:government-gateway",
          "email",
          "agentInformation",
          "openid:mdtp"
        )
      )
      willAuthoriseWith(200)

      And("The Auth token has a NINO")
      willReturnAuthorityWith(Nino(nino))

      And("The authority has enrolments")
      willReturnEnrolmentsWith()

      And("The auth will user details call will fail with 400")
      willFindUserFailed(409)

      When("We request the user information")
      val result = Http(s"$serviceUrl")
        .options(HttpOptions.readTimeout(1000000), HttpOptions.connTimeout(1000000))
        .headers(
          Seq(
            "Authorization"                -> s"Bearer $authorizationTokens",
            "Accept"                       -> "application/vnd.hmrc.1.0+json",
            "token"                        -> "ggToken",
            "X-Client-Authorization-Token" -> "ACCESS_TOKEN"
          )
        )
        .asString

      Then("The user information is returned")
      result.code shouldBe 502

    }

    Scenario("fetching user info returns 401 then we return 401 when X-Client-Authorization-Token header missing") {

      Given(
        "A Auth token with 'openid', 'profile', 'address', 'openid:gov-uk-identifiers', 'openid:hmrc-enrolments', 'openid:mdtp'," +
          "'email' and 'openid:government-gateway' scopes"
      )
      willReturnScopesForAuthTokens(
        accessToken,
        Set(
          "openid",
          "profile",
          "address",
          "openid:gov-uk-identifiers",
          "openid:hmrc-enrolments",
          "openid:government-gateway",
          "email",
          "agentInformation",
          "openid:mdtp"
        )
      )
      willAuthoriseWith(200)

      And("The Auth token has a NINO")
      willReturnAuthorityWith(Nino(nino))

      And("The authority has enrolments")
      willReturnEnrolmentsWith()

      And("The auth will user details call will fail with 400")
      willFindUserFailed(409)

      When("We request the user information without the X-Client-Authorization-Token header")
      val result = Http(s"$serviceUrl")
        .options(HttpOptions.readTimeout(1000000), HttpOptions.connTimeout(1000000))
        .headers(
          Seq(
            "Authorization" -> s"Bearer $authorizationTokens",
            "Accept"        -> "application/vnd.hmrc.1.0+json",
            "token"         -> "ggToken"
          )
        )
        .asString

      Then("The user information is not returned")
      result.code shouldBe 401

    }

  }

}
